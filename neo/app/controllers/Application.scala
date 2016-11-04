package controllers

import java.io.{BufferedReader, File, FileInputStream, FileReader}
import javax.inject.{Inject, Singleton}

import actor.NeoActor
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

  val cities = Application.loadCity(environment.getFile("/public/data/city.json"))
  val config = new Config(configuration)
  val asterixConn = new AsterixConn(config.AsterixURL, wsClient)

  val loadMeta = Await.result(Migration_20160814.migration.up(asterixConn), 10.seconds)

  val manager = system.actorOf(DataStoreManager.props(Migration_20160814.berryMeta, asterixConn, AQLGenerator, config))

  val berryProp = BerryClient.props(new JSONParser(), manager, new QueryPlanner(), config)
  val berryClient = system.actorOf(berryProp)

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
    val prop = BerryClient.props(new JSONParser(), manager, new QueryPlanner(), config)
    ActorFlow.actorRef(out => NeoActor.props(out, prop))
  }

  def tweet(id: String) = Action.async {
    val url = "https://api.twitter.com/1/statuses/oembed.json?id=" + id
    wsClient.url(url).get().map { response =>
      Ok(response.json)
    }
  }



  def getCity(NELat: Double, SWLat: Double, NELng: Double, SWLng: Double) = Action {
//TODO: Do binary search
    val citiesWithinBoundary = cities.filter{
      city =>
        (city \ "centroidY").as[Double] <= NELat && (city \ "centroidY").as[Double] >= SWLat.toDouble && (city \ "centroidX").as[Double] <= NELng.toDouble && (city \ "centroidX").as[Double] >= SWLng.toDouble
    }
    val header = Json.parse("{\"type\": \"FeatureCollection\"}").as[JsObject]
    val response = header + ("features" -> Json.toJson(citiesWithinBoundary))
    Ok(Json.toJson(response))
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

object Application{
  def loadCity(file: File): List[JsValue] = {
    val stream = new FileInputStream(file)
    val json = Json.parse(stream)
    val features = "features"
    val geometry = "geometry"
    val type_str = "type"
    val coordinates = "coordinates"
    val Polygon = "Polygon"
    val MultiPolyton = "MultiPolygon"
    val values = (json \ features).as[List[JsObject]]
    val newValues = List.newBuilder[JsValue]
    for(n <- values.indices) {
      val thisValue = values.apply(n)
      val geoType = (thisValue \ geometry \ type_str).as[String]

      geoType match{
        case Polygon => {
          val corr  = (thisValue \ geometry \ coordinates).as[JsArray].apply(0).as[List[List[Double]]]
          val Xcorr = corr.map( x => x.apply(0))
          val Ycorr = corr.map( x => x.apply(1))
          val minX = Xcorr.reduceLeft((x, y) => if (x < y) x else y)
          val maxX = Xcorr.reduceLeft((x, y) => if (x > y) x else y)
          val minY = Ycorr.reduceLeft((x, y) => if (x < y) x else y)
          val maxY = Ycorr.reduceLeft((x, y) => if (x > y) x else y)
          val thisX = (minX + maxX) / 2
          val thisY = (minY + maxY) / 2
          val newV = thisValue + ("centroidX" -> Json.toJson(thisX)) + ("centroidY" -> Json.toJson(thisY))
          newValues += Json.toJson(newV)
        }
        case MultiPolyton => {
          val allCorr = (thisValue \ "geometry" \ "coordinates").as[JsArray]
          val builderX = List.newBuilder[Double]
          val builderY = List.newBuilder[Double]
          for(i <- allCorr.value.indices){
            val rawCorr = allCorr.apply(i).as[JsArray]
            val realCorr = rawCorr.apply(0).as[List[List[Double]]]
            realCorr.map(x => builderX += x.apply(0))
            realCorr.map(x => builderY += x.apply(1))
          }
          val Xcorr = builderX.result()
          val Ycorr = builderY.result()
          val minX = Xcorr.reduceLeft((x, y) => if (x < y) x else y)
          val maxX = Xcorr.reduceLeft((x, y) => if (x > y) x else y)
          val minY = Ycorr.reduceLeft((x, y) => if (x < y) x else y)
          val maxY = Ycorr.reduceLeft((x, y) => if (x > y) x else y)
          val thisX = (minX + maxX) / 2
          val thisY = (minY + maxY) / 2
          val newV = thisValue + ("centroidX" -> Json.toJson(thisX)) + ("centroidY" -> Json.toJson(thisY))
          newValues += Json.toJson(newV)
        }
        case _ =>{
          val minX, minY, maxX, maxY = 0
          //FIXME: change throw to log
          throw new IllegalArgumentException("Unidentified geometry type in city.json");
        }
      }
    }

    newValues.result().sortWith((x,y) => (x\"centroidX").as[Double] < (y\"centroidX").as[Double])
  }
}
