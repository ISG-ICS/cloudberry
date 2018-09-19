package actor

import akka.actor.{Actor, ActorLogging, Props}
import akka.stream.Materializer
import edu.uci.ics.cloudberry.zion.actor.BerryClient._
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{ICategoricalTransform, IPostTransform, NoTransform}
import play.api.libs.json._
import play.api.Logger
import play.api.mvc.RequestHeader

import scala.concurrent.ExecutionContext

class RequestRouter (clientProp: Props, config: Config, requestHeader: RequestHeader)
                    (implicit ec: ExecutionContext, implicit val materializer: Materializer) extends Actor with ActorLogging {

  import RequestRouter._

  val client = context.actorOf(clientProp)
  val clientLogger = Logger("client")

  override def receive: Receive = {
    case requestBody: JsValue =>
      val remoteAddress = requestHeader.remoteAddress
      val userAgent = requestHeader.headers.get("user-agent").getOrElse("unknown")
      clientLogger.info(s"Request: user-IP = $remoteAddress; user-agent = $userAgent; user-query = ${requestBody.toString}")

      val transformer = parseTransform(requestBody)
      val clientRequestBody = getBerryRequest(requestBody)
      client ! (clientRequestBody, transformer)
    case e =>
      log.error("Unknown type of request: " + e)
  }

  private def parseTransform(requestBody: JsValue): IPostTransform = {
    (requestBody \ "transform").asOpt[JsValue] match {
      case Some(t) =>
        (t \ "wrap").asOpt[JsValue] match {
          case Some(w) => WrapTransform((w \"id").as[String], (w \"category").as[String])
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

}

object RequestRouter {

  def props(clientProp: Props, config: Config, requestHeader: RequestHeader)
           (implicit ec: ExecutionContext, materializer: Materializer) = Props(new RequestRouter(clientProp, config, requestHeader))

  case class WrapTransform(id: String, category: String) extends ICategoricalTransform{
    override def transform(jsonBody: JsValue): JsValue = {
      Json.obj("id" -> id, "category" -> category, "value" -> jsonBody)
    }
  }

}