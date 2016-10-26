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

  val cities = loadCity()

  println(cities.apply(0))

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

  def loadCity() : List[JsValue] = {
    val file = environment.getFile("/public/data/city.sample.shorten.json")
    val stream = new FileInputStream(file)
    val json = Json.parse(stream)
    val values = (json \ "features").as[List[JsObject]]
    var newValues = List[JsValue]()
    for(n <- values.indices) {
      val thisValue = values.apply(n)
      val geoType = (thisValue \ "geometry" \ "type").as[String]
      var sumX = 0.0
      var sumY = 0.0
      var length = 0.0
      if(geoType == "Polygon") {
        val rawCorr = (thisValue \ "geometry" \ "coordinates").as[JsArray]
        val realCorr = rawCorr.apply(0).as[JsArray]
        for(i <- realCorr.value.indices){
          sumX += realCorr.apply(i).apply(0).as[Double]
          sumY += realCorr.apply(i).apply(1).as[Double]
        }
        length = realCorr.value.size
      } else if(geoType == "MultiPolygon") {
        val allCorr = (thisValue \ "geometry" \ "coordinates").as[JsArray]
        for(i <- allCorr.value.indices){
          val rawCorr = allCorr.apply(i).as[JsArray]
          val realCorr = rawCorr.apply(0).as[JsArray]
          for(j <- realCorr.value.indices){
            sumX += realCorr.apply(j).apply(0).as[Double]
            sumY += realCorr.apply(j).apply(1).as[Double]
          }
          length += realCorr.value.size
        }
      } else {
        throw new IllegalArgumentException("Unidentified geometry type in city.json");
      }
      val thisX = sumX / length
      val thisY = sumY / length
      val newV = thisValue + ("centroidX" -> Json.toJson(thisX)) + ("centroidY" -> Json.toJson(thisY))
      newValues ::= Json.toJson(newV)
    }
    //TODO: sort newValues based on centroidX
    return newValues
  }

  def getCity(NELat: String, SWLat: String, NELng: String, SWLng: String) = Action {
//TODO: Do binary search
    var citiesWithinBoundary = List[JsValue]()
    println(NELat.toDouble,SWLat.toDouble,NELng.toDouble,SWLng.toDouble)
    for (v <- cities){
      val centroidX = (v \ "centroidX").as[Double]
      val centroidY = (v \ "centroidY").as[Double]
      println(centroidX,centroidY)
      if(centroidY <= NELat.toDouble && centroidY >= SWLat.toDouble && centroidX <= NELng.toDouble && centroidX >= SWLng.toDouble) {
        println("added")
        citiesWithinBoundary ::= v
      }
    }
    val response = Json.parse("{\"type\": \"FeatureCollection\"}").as[JsObject]
    val responseN = response + ("features" -> Json.toJson(citiesWithinBoundary))
    Ok(Json.toJson(responseN))
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
