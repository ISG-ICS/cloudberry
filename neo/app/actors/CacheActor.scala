package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import edu.uci.ics.cloudberry.gnosis.USGeoGnosis.USGeoTagInfo
import edu.uci.ics.cloudberry.gnosis.{IEntity, IUSGeoJSONEntity, TypeLevel, USGeoGnosis}
import models.{DataSet, QueryResult}
import org.joda.time.{DateTime, Interval}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * There is one cache per keyword
  */
class CacheActor(val viewsActor: ActorRef, val usGeoGnosis: USGeoGnosis)
                (val dataSet: DataSet, val keyword: String)
  extends Actor with ActorLogging {

  @volatile
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

    import scala.concurrent.duration._
    implicit val timeout = Timeout(5.seconds)

    (viewsActor ? setQuery).mapTo[Seq[QueryResult]] onComplete {
      case Success(viewAnswer: Seq[QueryResult]) => {
        viewAnswer.foreach { vr =>
          log.info("cache send" + vr + " to user:" + sender)
          sender ! cachedAnswer + vr
          updateCache(setQuery, vr)
        }
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

// only one keyword considered so far
case class CacheQuery(dataSet: DataSet,
                      keyword: String,
                      timeRange: Interval,
                      level: TypeLevel,
                      entities: Seq[IEntity],
                      repeatDuration: Duration = 0.seconds) {
  val key = dataSet.name + '_' + keyword
  override val toString = s"dataset:${dataSet.name},keyword:$keyword,timeRange:$timeRange," +
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

