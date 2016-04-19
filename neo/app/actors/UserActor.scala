package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.event.LoggingReceive
import akka.util.Timeout
import edu.uci.ics.cloudberry.gnosis._
import models.{DataSet, QueryResult}
import org.joda.time.{DateTime, Interval}
import play.api.libs.json._

import scala.concurrent.duration._

/**
  * Each user is an actor.
  */
class UserActor(val out: ActorRef, val cachesActor: ActorRef, val usGeoGnosis: USGeoGnosis) extends Actor with ActorLogging {

  implicit val timeout = Timeout(5.seconds)

  def receive() = LoggingReceive {
    case json: JsValue => {
      val setQuery = parseQuery(json)
      // why tell instead of ask? because we want to send the query continuously.
      // The ask model is answered once, the rest of responds will be send to dead letter unless we send the out to msg
      log.info("query is:" + setQuery)
      if (setQuery.entities.length > 0) {
        cachesActor.tell(setQuery, self)
      } else {
        out ! Json.obj("aggType" -> "error", "errorMessage" -> "no spatial area covered in this area")
      }
    }
    case result: QueryResult =>
      out ! Json.toJson(result)
    case other =>
  }

  def parseQuery(json: JsValue) :CacheQuery= {
    import UserActor._
    val userQuery = json.as[UserQuery]
    val level = matchLevel(userQuery.level)
    val entities = usGeoGnosis.tagRectangle(level, userQuery.area)
    CacheQuery(DataSet.Twitter, userQuery.keyword, userQuery.timeRange, level, entities, (userQuery.repeatDuration).seconds)
  }

}

object UserActor {
  def props(out: ActorRef, cachesActor: ActorRef, gnosis: USGeoGnosis) = Props(new UserActor(out, cachesActor, gnosis))

  def matchLevel(levelString: String): TypeLevel = {
    levelString.toLowerCase match {
      case "state" => StateLevel
      case "county" => CountyLevel
      case "city" => CityLevel
      case _ => StateLevel
    }
  }

  def matchDataSet(dataset: String): DataSet = {
    dataset.toLowerCase match {
      case "twitter" => DataSet.Twitter
      case _ => DataSet.Twitter
    }
  }
}

//TODO add the aggregation requirement parameters. Currently we calculate all the registered aggregation functions.
case class UserQuery(dataset: String,
                     keyword: String,
                     timeRange: Interval,
                     area: Rectangle,
                     level: String,
                     repeatDuration: Long = 0
                    )

object UserQuery {

  val Sample = UserQuery(DataSet.Twitter.name,
                         "rain",
                         new Interval(new DateTime(2012, 1, 1, 0, 0).getMillis(), new DateTime(2016, 3, 1, 0, 0).getMillis()),
                         Rectangle(-146.95312499999997, 7.798078531355303, -45.703125, 61.3546135846894),
                         level = "state")

  implicit val intervalFormat: Format[Interval] = {
    new Format[Interval] {
      override def writes(interval: Interval): JsValue = {
        JsObject(Seq(("start", JsNumber(interval.getStartMillis)), ("end", JsNumber(interval.getEndMillis))))
      }

      override def reads(json: JsValue): JsResult[Interval] = {
        JsSuccess(new Interval((json \ "start").as[Long], (json \ "end").as[Long]))
      }
    }
  }

  implicit val userQueryFormat: Format[UserQuery] = Json.format[UserQuery]
}

