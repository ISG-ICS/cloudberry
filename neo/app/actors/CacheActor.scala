package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import edu.uci.ics.cloudberry.gnosis.USGeoGnosis.USGeoTagInfo
import edu.uci.ics.cloudberry.gnosis._
import edu.uci.ics.cloudberry.zion.model._
import org.joda.time.{DateTime, Interval}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.{Format, Json}

import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * There is one cache per keyword
  */
class CacheActor(val viewsActor: ActorRef, val usGeoGnosis: USGeoGnosis)
                (val dataSet: String, val keyword: Option[String])
  extends Actor with ActorLogging {

  var timeRange: Interval = new Interval(new DateTime(2012, 1, 1, 0, 0).getMillis, DateTime.now().getMillis)

  def receive = {
    case q: CacheQuery if q.entities.size > 0 =>
      val cachedAnswer: QueryResult = answerAsMuchAsICan(q)
      splitQuery(q) match {
        case Some(viewQuery) =>
          mergeAnswerFromView(viewQuery, cachedAnswer, sender())
        case None =>
          sender() ! cachedAnswer
      }
    case update: Interval =>
      timeRange = update
  }

  def answerAsMuchAsICan(q: CacheQuery): QueryResult = {
    QueryResult.Empty
  }

  def splitQuery(q: CacheQuery): Option[CacheQuery] = Some(q)

  def mergeAnswerFromView(setQuery: CacheQuery, cachedAnswer: QueryResult, sender: ActorRef) = {

    import akka.pattern.ask
    import CacheActor.timeout

    //FIXME hard code now
    val dbQuery: DBQuery =
      if (keyword.isDefined) {
        DBQuery(SummaryLevel(SpatialLevels.State, TimeLevels.Day), Seq[Predicate](new KeywordPredicate("", Seq(keyword.get))))
      } else {
        DBQuery(SummaryLevel(SpatialLevels.State, TimeLevels.Day), Seq.empty[Predicate])
      }
    (viewsActor ? dbQuery).mapTo[SpatialTimeCount] onComplete {
      case Success(viewAnswer) => {
        sender ! QueryResult("map", viewAnswer.map)
        sender ! QueryResult("time", viewAnswer.time)
        sender ! QueryResult("hashtag", viewAnswer.hashtag)

//          sender ! cachedAnswer + vr
//          updateCache(setQuery, vr)
      }
      case Failure(e: Throwable) => {
        log.error(e, "cache failed")
      }
    }
  }

  def updateCache(q: CacheQuery, viewAnswer: QueryResult) = {
    self ! q.timeRange
  }
}

object CacheActor {
  implicit val timeout: Timeout = Timeout(5.seconds)
}

// only one keyword considered so far
case class CacheQuery(dataSet: String,
                      keyword: Option[String],
                      timeRange: Interval,
                      level: TypeLevel,
                      entities: Seq[IEntity],
                      repeatDuration: Duration = 0.seconds) {
  val key = dataSet + '_' + keyword.getOrElse("")
  override val toString = s"dataset:${dataSet},keyword:$keyword,timeRange:$timeRange," +
    s"level:$level,entities:${entities.map(e => USGeoTagInfo.apply(e.asInstanceOf[IUSGeoJSONEntity]))}"
}

class CachesActor(val viewsActor: ActorRef, val usGeoGnosis: USGeoGnosis) extends Actor with ActorLogging {
  def receive = {
    case q: CacheQuery => {
      log.info("Caches:" + self + " get query from : " + sender())
      context.child(q.key).getOrElse {
        context.actorOf(Props(new CacheActor(viewsActor, usGeoGnosis)(q.dataSet, q.keyword)), q.key)
      } forward q
    }
    case other =>
      log.info("Caches:" + self + "receive:" + other + " from : " + sender())
  }
}

case class QueryResult(aggType: String, result: Seq[KeyCountPair])

object QueryResult {
  val Empty = QueryResult("", Seq.empty[KeyCountPair])
  implicit val format: Format[QueryResult] = Json.format[QueryResult]
}

