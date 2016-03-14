package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import com.esri.core.geometry.Polygon
import models.QueryResult
import org.joda.time.Interval
import play.Play
import play.api.libs.json.{JsValue, Json}

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Each user is an actor.
  */
class UserActor(out: ActorRef) extends Actor with ActorLogging {
  val list: Seq[String] = Play.application().configuration().getStringList("default.stocks")
  list.foreach(StocksActor.stocksActor ! WatchStock(_))

  implicit val query = Json.reads[RESTFulQuery]

  implicit val timeout = 5.seconds

  def parseQuery(rESTFulQuery: RESTFulQuery) = {
    val entities = Knowledge.geoTag(new Polygon(), rESTFulQuery.level)
    ParsedQuery(rESTFulQuery.keyword, new Interval(rESTFulQuery.timeStart, rESTFulQuery.timeEnd), entities)
  }

  def receive(): Unit = {
    case json: JsValue =>
      val parsedQuery = parseQuery(json.as[RESTFulQuery])
      (CachesActor.cachesActor ? parsedQuery).mapTo[QueryResult] onComplete {
        case Success(result) => sender ! result
        case Failure(t) => sender ! t
      }
    case other =>
      log.info("received:" + other)
  }
}

object UserActor {
  def props(out: ActorRef) = Props(new UserActor(out))
}

case class RESTFulQuery(keyword: String,
                        timeStart: Long,
                        timeEnd: Long,
                        leftBottomLog: Double,
                        leftBottomLat: Double,
                        rightTopLog: Double,
                        rightTopLat: Double,
                        level: Int
                       )

case class ParsedQuery(keyword: String, timeRange: Interval, entities: Seq[String])
