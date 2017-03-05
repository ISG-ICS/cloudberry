package actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.common.Config
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.RequestHeader
import play.Logger

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class RequestRouter (out: ActorRef, ws: WSClient, requestHeader: RequestHeader, berryClientProp: Props, config: Config)
                    (implicit ec: ExecutionContext, implicit val materializer: Materializer) extends Actor with ActorLogging {

  import RequestRouter._
  import KeyType._

  implicit val timeout: Timeout = Timeout(20.minutes)
  val StreamingBerryClient = context.actorOf(berryClientProp)
  val nonStreamingBerryClient = context.actorOf(berryClientProp)

  // TODO UserRequest in package model is of no use
  // TODO change error treatment
  override def receive: Receive = {
    case requestBody: JsValue =>
      (requestBody \ "transform" \\ "key")(0).asOpt[KeyType.Value] match {
        case Some(TotalCount) =>
          (nonStreamingBerryClient ? (requestBody \ "transform" \\ "value")(0)).mapTo[JsValue].map{
            json => out ! respondWrapper(json, TotalCount)
          }
        case Some(Sample) =>
          (nonStreamingBerryClient ? (requestBody \ "transform" \\ "value")(0)).mapTo[JsValue].map{
            json => out ! respondWrapper(json, Sample)
          }
        case Some(Batch) =>

          Logger.error("Request: " + (requestBody \ "transform" \\ "value")(0).toString())

          (StreamingBerryClient ? (requestBody \ "transform" \\ "value")(0)).mapTo[JsValue].map{
            json =>
              Logger.error("Respond: " + json.toString())
              out ! respondWrapper(json, Batch)
          }

        case None => Logger.error("Unknown request Type.")
      }
    case e => Logger.error("unknown type of request " + e)
  }

  private def respondWrapper(response: JsValue, key: KeyType.Value): JsValue = {
    Json.obj(
      "transform" -> Json.obj(
        "wrap" -> Json.obj(
          "key" -> key,
          "value" -> response
        )
      )
    )
  }

}

object RequestRouter {

  def props(out: ActorRef, ws: WSClient, requestHeader: RequestHeader, berryClientProp: Props, config: Config)
           (implicit ec: ExecutionContext, materializer: Materializer) = Props(new RequestRouter(out, ws, requestHeader, berryClientProp, config))

  object KeyType extends Enumeration {
    val Sample = Value("sample")
    val Batch = Value("batch")
    val TotalCount = Value("totalCount")
  }

  def enumerationReader[E <: Enumeration](enum: E) = new Reads[enum.Value] {
    override def reads(json: JsValue): JsResult[enum.Value] = {
      val key = json.as[String]
      enum.values.find(_.toString == key) match {
        case Some(value) => JsSuccess(value)
        case None => JsError(s"$key not found in enum: $enum")
      }
    }
  }

  implicit val requestTypeReader: Reads[KeyType.Value] = enumerationReader(KeyType)
}