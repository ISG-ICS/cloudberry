package actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.Materializer
import edu.uci.ics.cloudberry.zion.actor.BerryClient._
import edu.uci.ics.cloudberry.zion.common.Config
import play.api.libs.json._
import play.Logger

import scala.concurrent.ExecutionContext

class RequestRouter (out: ActorRef, berryClientProp: Props, config: Config)
                    (implicit ec: ExecutionContext, implicit val materializer: Materializer) extends Actor with ActorLogging {

  import RequestRouter._

  val streamingBerryClient = context.actorOf(berryClientProp)
  val nonStreamingBerryClient = context.actorOf(berryClientProp)

  override def receive: Receive = {
    case requestBody: JsValue =>
      val transformer = parseTransform(requestBody)
      val berryRequestBody = getBerryRequest(requestBody)
      (berryRequestBody \\ "sliceMillis").isEmpty match {
        case true => handleNonStreamingBody(berryRequestBody, transformer)
        case false => handleStreamingBody(berryRequestBody, transformer)
      }
    case e =>
      Logger.error("Unknown type of request: " + e)
  }

  private def parseTransform(requestBody: JsValue): IPostTransform = {
    (requestBody \ "transform").asOpt[JsValue] match {
      case Some(t) =>
        (t \ "wrap").asOpt[JsValue] match {
          case Some(w) => WrapTransform((w \"key").as[String])
          case None => NoTransform
        }
      case None => NoTransform
    }
  }

  private def getBerryRequest(requestBody: JsValue): JsValue = {
    (requestBody \ "transform").asOpt[JsValue] match {
      case Some(_) => requestBody.as[JsObject] - "transform"
      case None => requestBody
    }
  }

  private def handleNonStreamingBody(requestBody: JsValue, transform: IPostTransform): Unit = {
    nonStreamingBerryClient ! Request(requestBody, transform)
  }

  private def handleStreamingBody(requestBody: JsValue, transform: IPostTransform): Unit = {
    streamingBerryClient ! Request(requestBody, transform)
  }

}

object RequestRouter {
  def props(out: ActorRef, berryClientProp: Props, config: Config)
           (implicit ec: ExecutionContext, materializer: Materializer) = Props(new RequestRouter(out, berryClientProp, config))

  case class WrapTransform(key: String) extends IPostTransform {
    override def transform(jsonBody: JsValue): JsValue = {
      Json.obj("key" -> key, "value" -> jsonBody)
    }
  }

}