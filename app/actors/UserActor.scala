package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import com.esri.core.geometry.Polygon
import org.joda.time.Interval
import play.api.libs.json.{JsValue, Json}

import scala.concurrent.duration._

/**
  * Each user is an actor.
  */
class UserActor(out: ActorRef) extends Actor with ActorLogging {

  implicit val timeout = Timeout(5.seconds)


  def parseQuery(rESTFulQuery: RESTFulQuery) = {
    val entities = Knowledge.geoTag(new Polygon(), rESTFulQuery.level)
    ParsedQuery(rESTFulQuery.keyword, new Interval(rESTFulQuery.timeStart, rESTFulQuery.timeEnd), entities)
  }

  def receive() = {
    case json: JsValue =>
      out ! Json.toJson(Seq(Map("text" -> "fake one"), Map("text" -> "fake two")))
    //val parsedQuery = parseQuery(json.as[RESTFulQuery](RESTFulQuery.restfulQueryFormat))
    //(CachesActor.cachesActor ? parsedQuery).mapTo[QueryResult] onComplete {
    //  case Success(result) => sender ! result
    //  case Failure(t) => sender ! t
    //}
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
  implicit val restfulQueryWriter = Json.writes[RESTFulQuery]
  implicit val restfulQueryFormat = Json.format[RESTFulQuery]
}

case class ParsedQuery(keyword: String, timeRange: Interval, entities: Seq[String])
