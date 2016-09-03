package actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import edu.uci.ics.cloudberry.zion.actor.ReactiveBerryClient
import models.UserRequest
import play.api.libs.json.{JsArray, JsError, JsValue, Json}

class NeoReactiveActor(out: ActorRef, val berryClientProps: Props) extends Actor with ActorLogging {

  val berryClient = context.watch(context.actorOf(berryClientProps))

  import actor.NeoReactiveActor.NeoTransformer

  override def receive: Receive = {
    case json: JsValue =>
      json.validate[UserRequest].map { userRequest =>
        val groupRequest = NeoActor.generateCBerryRequest(userRequest).map { case (reqType, request) =>
          (request, NeoTransformer(reqType.toString))
        }.toSeq
        berryClient.tell(ReactiveBerryClient.Request(groupRequest), out)
      }.recoverTotal {
        e => sender ! JsError.toJson(e)
      }
  }
}

object NeoReactiveActor {
  def props(out: ActorRef, berryClientProp: Props) = Props(new NeoReactiveActor(out, berryClientProp))

  case class NeoTransformer(key: String) extends ReactiveBerryClient.IPostTransform {
    override def transform(jsValue: JsValue): JsValue = {
      Json.obj("key" -> key, "value" -> jsValue)
    }
  }

}
