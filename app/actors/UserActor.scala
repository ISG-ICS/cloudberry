package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import com.esri.core.geometry.Polygon
import models.QueryResult
import org.joda.time.{DateTime, Interval}
import play.api.libs.json.{JsValue, Json}

/**
  * Each user is an actor.
  */
class UserActor(out: ActorRef) extends Actor with ActorLogging {

  import scala.concurrent.duration._

  implicit val timeout = Timeout(5.seconds)

  def parseQuery(json: JsValue) = {
    //    val rESTFulQuery = json.as[RESTFulQuery]
    val rESTFulQuery = RESTFulQuery.Sample
    val entities = Knowledge.geoTag(new Polygon(), rESTFulQuery.level)
    ParsedQuery(rESTFulQuery.keyword, new Interval(rESTFulQuery.timeStart, rESTFulQuery.timeEnd), entities)
  }

  def receive() = {
    case json: JsValue =>
      out ! Json.toJson(QueryResult.Sample)
    //      val parsedQuery = parseQuery(json)
    //      (CachesActor.cachesActor ? parsedQuery).mapTo[QueryResult] onComplete {
    //        case Success(result) => out ! result
    //        case Failure(t) => out ! t
    //      }
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

object RESTFulQuery {

  val Sample = RESTFulQuery("rain",
    new DateTime(2012, 1, 1, 0, 0).getMillis(),
    new DateTime(2016, 3, 1, 0, 0).getMillis(),
    -146.95312499999997,
    7.798078531355303,
    -45.703125,
    61.3546135846894,
    1
  )
  implicit val restfulQueryWriter = Json.writes[RESTFulQuery]
  implicit val restfulQueryFormat = Json.format[RESTFulQuery]
}


case class ParsedQuery(keyword: String, timeRange: Interval, entities: Seq[String])
