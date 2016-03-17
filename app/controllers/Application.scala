package controllers

import javax.inject.{Inject, Singleton}

import actors.{CachesActor, Knowledge, RESTFulQuery, UserActor}
import akka.actor.ActorSystem
import akka.util.Timeout
import play.api.Play.current
import play.api.Play.materializer
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.api.mvc._

@Singleton
class Application @Inject()(wsClient: WSClient, system: ActorSystem) extends Controller {

  Knowledge.loadFromDB

  import akka.pattern.ask

  import scala.concurrent.duration._

  implicit val timeout = Timeout(5.seconds)

  def index = Action {
    Ok(views.html.index("Cloudberry"))
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
