package actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.Materializer
import edu.uci.ics.cloudberry.zion.common.Config
import play.api.libs.json._
import play.api.libs.ws.WSClient
import play.api.mvc.RequestHeader
import play.Logger

import scala.concurrent.ExecutionContext

class RequestForwarder (out: ActorRef, ws: WSClient, requestHeader: RequestHeader, config: Config)
                       (implicit ec: ExecutionContext, implicit val materializer: Materializer) extends Actor with ActorLogging {

  override def receive: Receive = {
    case requestBody: JsValue =>
      requestHeader.session.get("userID").map { userID =>
        val childName = s"userID-$userID"
        val child = context.child(childName).getOrElse {
          context.actorOf(NeoActor.props(out, ws, requestHeader.host, config), childName)
        }
        child ! requestBody

        Logger.error("this request is responsible by " + childName)

      }.getOrElse {
        Logger.error("no userID found.")
      }

    case e => Logger.error("unknown type of request " + e)
  }

}

object RequestForwarder {
  def props(out: ActorRef, ws: WSClient, requestHeader: RequestHeader, config: Config)
           (implicit ec: ExecutionContext, materializer: Materializer) = Props(new RequestForwarder(out, ws, requestHeader, config))
}