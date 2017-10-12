package actor

import akka.actor._
import akka.stream.Materializer
import play.api.libs.json.JsValue

import scala.concurrent.ExecutionContext

class TwitterMapPigeon (val out: ActorRef)
                       (implicit ec: ExecutionContext, implicit val materializer: Materializer) extends Actor with ActorLogging {

  override def receive: Receive = {
    case body: JsValue =>
      log.error(body.toString())
  }
}

object TwitterMapPigeon {
  def props(out: ActorRef)
           (implicit ec: ExecutionContext, materializer: Materializer) = Props(new TwitterMapPigeon(out))
}