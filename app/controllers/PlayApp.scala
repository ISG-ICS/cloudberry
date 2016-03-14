package controllers

import javax.inject.{Inject, Singleton}

import actors.{Knowledge, CachesActor, RESTFulQuery, UserActor}
import akka.actor.ActorSystem
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.api.mvc._

@Singleton
class PlayApp @Inject()(wsClient: WSClient, system: ActorSystem) extends Controller {

  Knowledge.loadFromDB

  import scala.concurrent.duration._
  import akka.pattern.ask

  implicit val timeout = 5.seconds

  def index = Action {
    Ok("Hello world")
  }

  def hello(name: String) = Action(parse.json) { request =>
    val json = request.body.validate[String]
    Ok("Hello " + name)
  }

  def ws = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
    UserActor.props(out)
  }

  def search(query: JsValue) = Action.async {
    (CachesActor.cachesActor ? query.as[RESTFulQuery]).mapTo[JsValue].map { answer =>
      Ok(answer)
    }
  }

}
