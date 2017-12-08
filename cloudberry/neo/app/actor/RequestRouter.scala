package actor

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.Materializer
import edu.uci.ics.cloudberry.zion.actor.BerryClient._
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IPostTransform, NoTransform}
import play.api.libs.json._
import play.api.Logger
import play.api.mvc.RequestHeader

import scala.concurrent.ExecutionContext

class RequestRouter (berryClientProp: Props, config: Config, requestHeader: RequestHeader)
                    (implicit ec: ExecutionContext, implicit val materializer: Materializer) extends Actor with ActorLogging {

  import RequestRouter._

  val streamingBerryClient = context.actorOf(berryClientProp, streamingClientName)
  val nonStreamingBerryClient = context.actorOf(berryClientProp, nonStreamingClientName)
  val clientLogger = Logger("client")

  override def receive: Receive = {
    case requestBody: JsValue =>
      val remoteAddress = requestHeader.remoteAddress
      val userAgent = requestHeader.headers.get("user-agent").getOrElse("unknown")
      clientLogger.info(s"Request: user-IP = $remoteAddress; user-agent = $userAgent; user-query = ${requestBody.toString}")

      val transformer = parseTransform(requestBody)
      val berryRequestBody = getBerryRequest(requestBody)
      (berryRequestBody \\ "sliceMillis").isEmpty match {
        case true => handleNonStreamingBody(berryRequestBody, transformer)
        case false => handleStreamingBody(berryRequestBody, transformer)
      }
    case e =>
      log.error("Unknown type of request: " + e)
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
    nonStreamingBerryClient ! (requestBody, transform)
  }

  private def handleStreamingBody(requestBody: JsValue, transform: IPostTransform): Unit = {
    streamingBerryClient ! (requestBody, transform)
  }

}

object RequestRouter {
  val streamingClientName = "streamingClient"
  val nonStreamingClientName = "nonStreamingClient"

  def props(berryClientProp: Props, config: Config, requestHeader: RequestHeader)
           (implicit ec: ExecutionContext, materializer: Materializer) = Props(new RequestRouter(berryClientProp, config, requestHeader))

  case class WrapTransform(key: String) extends IPostTransform {
    override def transform(jsonBody: JsValue): JsValue = {
      Json.obj("key" -> key, "value" -> jsonBody)
    }
  }

}