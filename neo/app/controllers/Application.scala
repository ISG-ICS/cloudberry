package controllers

import javax.inject.{Inject, Singleton}

import actor.{NeoActor, NeoReactiveActor}
import akka.actor.{Actor, ActorSystem, DeadLetter, PoisonPill, Props}
import akka.stream.{Materializer, OverflowStrategy}
import akka.stream.scaladsl.Source
import akka.util.Timeout
import db.Migration_20160814
import edu.uci.ics.cloudberry.zion.actor.{DataStoreManager, RESTFulBerryClient, ReactiveBerryClient}
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.AsterixConn
import edu.uci.ics.cloudberry.zion.model.impl.{AQLGenerator, JSONParser, QueryPlanner}
import edu.uci.ics.cloudberry.zion.model.schema.Query
import models.UserRequest
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsError, JsValue}
import play.api.libs.streams.ActorFlow
import play.api.libs.ws.WSClient
import play.api.mvc._
import play.api.{Configuration, Environment, Logger}

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

@Singleton
class Application @Inject()(val wsClient: WSClient,
                            val configuration: Configuration,
                            val environment: Environment)
                           (implicit val system: ActorSystem,
                            implicit val materializer: Materializer
                           ) extends Controller {

  val config = new Config(configuration)
  val asterixConn = new AsterixConn(config.AsterixURL, wsClient)

  val loadMeta = Await.result(Migration_20160814.migration.up(asterixConn), 10 seconds)

  val manager = system.actorOf(DataStoreManager.props(Migration_20160814.berryMeta, asterixConn, AQLGenerator, config))

  val berryProp = RESTFulBerryClient.props(new JSONParser(), manager, new QueryPlanner(), suggestView = true, config)
  val berryClient = system.actorOf(berryProp)
  val neoActor = system.actorOf(NeoActor.props(berryProp))

  Logger.logger.info("I'm initializing")

  val listener = system.actorOf(Props(classOf[Listener], this))
  system.eventStream.subscribe(listener, classOf[DeadLetter])

  def index = Action {
    Ok(views.html.index("Cloudberry"))
  }

  def debug = Action {
    Ok(views.html.debug("Debug"))
  }

  def dashboard = Action {
    Ok(views.html.dashboard("Dashboard"))
  }

  def ws = WebSocket.accept[JsValue, JsValue] { request =>
    //    ActorFlow.actorRef(out => NeoActor.props(out, berryProp))
    val prop = ReactiveBerryClient.props(new JSONParser(), manager, new QueryPlanner(), config, 1000)
    ActorFlow.actorRef(out => NeoReactiveActor.props(out, prop))
  }

  def tweet(id: String) = Action.async {
    val url = "https://api.twitter.com/1/statuses/oembed.json?id=" + id
    wsClient.url(url).get().map { response =>
      Ok(response.json)
    }
  }

  def neoQuery = Action.async(parse.json) { request =>
    import akka.pattern.ask
    implicit val timeout: Timeout = Timeout(config.UserTimeOut)

    request.body.validate[UserRequest].map { request =>
      (neoActor ? request.copy(mergeResult = true)).mapTo[JsValue].map(msg => Ok(msg))
    }.recoverTotal {
      e => Future(BadRequest("Detected error:" + JsError.toJson(e)))
    }
  }

  def berryQuery = Action.async(parse.json) { request =>
    import akka.pattern.ask
    implicit val timeout: Timeout = Timeout(config.UserTimeOut)

    import JSONParser._
    request.body.validate[Query].map { query: Query =>
      (berryClient ? query).mapTo[JsValue].map(msg => Ok(msg))
    }.recoverTotal {
      e => Future(BadRequest("Detected error:" + JsError.toJson(e)))
    }
  }

  class Listener extends Actor {
    def receive = {
      case d: DeadLetter => println(d)
    }
  }

}
