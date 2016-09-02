package actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import edu.uci.ics.cloudberry.zion.actor.ReactiveBerryClient
import models.UserRequest
import play.api.libs.json.{JsArray, JsError, JsValue, Json}

class NeoReactiveActor(out: ActorRef, val berryClientProps: Props) extends Actor with ActorLogging {

  val berryClient = context.watch(context.actorOf(berryClientProps))

  override def receive: Receive = {
    case json: JsValue =>
      json.validate[UserRequest].map { userRequest =>
        for (berryRequest <- NeoActor.generateCBerryRequest(userRequest)) {
          if (berryRequest._1 == NeoActor.RequestType.ByPlace) {
            val request = ReactiveBerryClient.Request(berryRequest._2,
                                                      (json: JsArray) => Json.obj("key" -> berryRequest._1.toString, "value" -> json))
            berryClient.tell(request, out)
          }
        }
      }.recoverTotal {
        e => sender ! JsError.toJson(e)
      }
  }
}

object NeoReactiveActor {
  def props(out: ActorRef, berryClientProp: Props) = Props(new NeoReactiveActor(out, berryClientProp))
}
