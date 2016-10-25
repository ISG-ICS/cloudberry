package controllers

import java.io.{BufferedReader, File, FileInputStream, FileReader}
import javax.inject.{Inject, Singleton}

import actor.{NeoActor, NeoActor$}
import akka.actor.{Actor, ActorSystem, DeadLetter, Props}
import akka.pattern.ask
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.Timeout
import db.Migration_20160814
import edu.uci.ics.cloudberry.zion.actor.{BerryClient, DataStoreManager}
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.AsterixConn
import edu.uci.ics.cloudberry.zion.model.impl.{AQLGenerator, JSONParser, QueryPlanner}
import edu.uci.ics.cloudberry.zion.model.schema.Query
import models.UserRequest
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
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

  val loadMeta = Await.result(Migration_20160814.migration.up(asterixConn), 10.seconds)

  val manager = system.actorOf(DataStoreManager.props(Migration_20160814.berryMeta, asterixConn, AQLGenerator, config))

  val berryProp = BerryClient.props(new JSONParser(), manager, new QueryPlanner(), config)
  val berryClient = system.actorOf(berryProp)

  Logger.logger.info("I'm initializing")

  val listener = system.actorOf(Props(classOf[Listener], this))
  system.eventStream.subscribe(listener, classOf[DeadLetter])

  //start loading city data
  val file = environment.getFile("/public/data/city.sample.json")
  val stream = new FileInputStream(file)
  val json = Json.parse(stream)
  val values = (json \ "features").as[List[JsValue]]
  for(v <- values) {
    val tp = (v \ "geometry" \ "type").as[String]
    var sumX = 0.0
    var sumY = 0.0
    var length = 0.0
    if(tp == "Polygon") {
      val rawCorr = (v \ "geometry" \ "coordinates").as[JsArray]
      val realCorr = rawCorr.apply(0).as[JsArray]
      for(i <- 0 to (realCorr.value.size - 1)){
        sumX += realCorr.apply(i).apply(0).as[Double]
        sumY += realCorr.apply(i).apply(1).as[Double]
      }
      length = realCorr.value.size
    } else if(tp == "MultiPolygon") {
      val allCorr = (v \ "geometry" \ "coordinates").as[JsArray]
      for(i <- 0 to (allCorr.value.size - 1)){
        val rawCorr = allCorr.apply(i).as[JsArray]
        val realCorr = rawCorr.apply(0).as[JsArray]
        for(j <- 0 to (realCorr.value.size - 1)){
          sumX += realCorr.apply(j).apply(0).as[Double]
          sumY += realCorr.apply(j).apply(1).as[Double]
        }
        length += realCorr.value.size
      }
    }
    println(length,sumX,sumY)
    //TODO: store the midpoints in order
  }

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
    val prop = BerryClient.props(new JSONParser(), manager, new QueryPlanner(), config)
    ActorFlow.actorRef(out => NeoActor.props(out, prop))
  }

  def tweet(id: String) = Action.async {
    val url = "https://api.twitter.com/1/statuses/oembed.json?id=" + id
    wsClient.url(url).get().map { response =>
      Ok(response.json)
    }
  }

  def city(NELat: String, SWLat: String, NELng: String, SWLng: String) = Action {
//TODO: Do binary search through the midpoints and return the satisfied ones
    Ok("test");
  }

  def berryQuery = Action.async(parse.json) { request =>
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
