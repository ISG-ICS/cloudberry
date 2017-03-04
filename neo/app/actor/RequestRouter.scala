package actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.Materializer
import edu.uci.ics.cloudberry.zion.common.Config
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.RequestHeader
import play.Logger

import scala.concurrent.ExecutionContext

class RequestRouter (out: ActorRef, ws: WSClient, requestHeader: RequestHeader, berryClientProp: Props, config: Config)
                    (implicit ec: ExecutionContext, implicit val materializer: Materializer) extends Actor with ActorLogging {

  val StreamingBerryClient = context.actorOf(berryClientProp)
  val nonStreamingBerryClient = context.actorOf(berryClientProp)

  // TODO Distribute different requests
  override def receive: Receive = {
    case requestBody: JsValue => ???

    case e => Logger.error("unknown type of request " + e)
  }

}

object RequestRouter {
  def props(out: ActorRef, ws: WSClient, requestHeader: RequestHeader, berryClientProp: Props, config: Config)
           (implicit ec: ExecutionContext, materializer: Materializer) = Props(new RequestRouter(out, ws, requestHeader, berryClientProp, config))
}