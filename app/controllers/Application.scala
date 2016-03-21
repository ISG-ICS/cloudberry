package controllers

import javax.inject.{Inject, Singleton}

import actors._
import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import play.api.Configuration
import play.api.Play.{current, materializer}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.api.mvc._

@Singleton
class Application @Inject()(system: ActorSystem,
                            implicit val wsClient: WSClient,
                            implicit val config: Configuration) extends Controller {

  Knowledge.loadFromDB
  lazy val viewsActor = system.actorOf(Props(classOf[ViewsActor], wsClient, config), "views")
  lazy val cachesActor = system.actorOf(Props(classOf[CachesActor], viewsActor), "caches")


  def index = Action {
    Ok(views.html.index("Cloudberry"))
  }

  def hello(name: String) = Action(parse.json) { request =>
    val json = request.body.validate[String]
    Ok("Hello " + name)
  }

  def ws = WebSocket.acceptWithActor[JsValue, JsValue] { request => out =>
    UserActor.props(out)(cachesActor)
  }

  def search(query: JsValue) = Action.async {
    import akka.pattern.ask

    import scala.concurrent.duration._
    implicit val timeout = Timeout(5.seconds)

    (cachesActor ? query.as[RESTFulQuery]).mapTo[JsValue].map { answer =>
      Ok(answer)
    }
  }

}
