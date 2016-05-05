package controllers

import javax.inject.{Inject, Singleton}

import actors._
import akka.actor.{Actor, ActorSystem, DeadLetter, Props}
import akka.util.Timeout
import db.{AQLConnection, Migration_20160324}
import edu.uci.ics.cloudberry.gnosis.USGeoGnosis
import edu.uci.ics.cloudberry.zion.asterix.AsterixConnection
import play.api.Play.{current, materializer}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import play.api.libs.streams.ActorFlow
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

@Singleton
class Application @Inject()(val wsClient: WSClient,
                            val config: Configuration,
                            val environment: Environment,
                            implicit val system: ActorSystem
                           ) extends Controller {

  val AsterixURL = config.getString("asterixdb.url").get
  val asterixConn = new AsterixConnection(wsClient, AsterixURL)

  Logger.logger.info("I'm initializing")
  val checkViewStatus = Migration_20160324(asterixConn).up()
  val USGeoGnosis = Knowledge.buildUSKnowledge(environment)

  Await.ready(checkViewStatus, 10 minute) onComplete {
    case Success(response: WSResponse) => Logger.logger.info(response.body)
    case Failure(ex) => Logger.logger.error(ex.getMessage); throw ex
  }

  val viewsActor = system.actorOf(Props(classOf[TwitterViewsManagerActor], asterixConn), "views")
  val cachesActor = system.actorOf(Props(classOf[CachesActor], viewsActor, USGeoGnosis), "caches")

  val listener = system.actorOf(Props(classOf[Listener], this))
  system.eventStream.subscribe(listener, classOf[DeadLetter])

  def index = Action {
    Ok(views.html.index("Cloudberry"))
    //    Ok(views.html.indexfull("Cloudberry"))
  }

  def ws = WebSocket.accept[JsValue, JsValue] { request =>
    ActorFlow.actorRef(out => UserActor.props(out, cachesActor, USGeoGnosis))
  }

  def search(query: JsValue) = Action.async {
    import akka.pattern.ask

    import scala.concurrent.duration._
    implicit val timeout = Timeout(5.seconds)

    (cachesActor ? query.as[UserQuery]).mapTo[JsValue].map { answer =>
      Ok(answer)
    }
  }

  class Listener extends Actor {
    def receive = {
      case d: DeadLetter => println(d)
    }
  }

}
