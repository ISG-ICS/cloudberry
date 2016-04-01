package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import models.{DataSet, QueryResult}
import org.joda.time.{DateTime, Interval}
import play.api.libs.concurrent.Execution.Implicits._

import scala.util.{Failure, Success}

/**
  * There is one cache per keyword
  */
class CacheActor(val viewsActor: ActorRef)(val dataSet: DataSet, val keyword: String) extends Actor with ActorLogging {

  @volatile
  var timeRange: Interval = new Interval(new DateTime(2012, 1, 1, 0, 0).getMillis, DateTime.now().getMillis)

  def receive = {
    case q: ParsedQuery =>
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

  def answerAsMuchAsICan(q: ParsedQuery): QueryResult = {
    QueryResult.Empty
  }

  def splitQuery(q: ParsedQuery): Option[ParsedQuery] = Some(q)

  def mergeAnswerFromView(parsed: ParsedQuery, cachedAnswer: QueryResult, sender: ActorRef) = {

    import akka.pattern.ask

    import scala.concurrent.duration._
    implicit val timeout = Timeout(3.seconds)

    (viewsActor ? parsed).mapTo[Seq[QueryResult]] onComplete {
      case Success(viewAnswer: Seq[QueryResult]) => {
        viewAnswer.foreach { vr =>
          log.info("cache send" + vr + " to user:" + sender)
          sender ! cachedAnswer + vr
          updateCache(parsed, vr)
        }
      }
      case Failure(e: Throwable) => {
        log.error(e, "cache failed")
      }
    }
  }

  def updateCache(q: ParsedQuery, viewAnswer: QueryResult) = {
    self ! q.timeRange
  }
}

// only one keyword consideraing so far
case class ParsedQuery(dataSet: DataSet, keyword: String, timeRange: Interval, entities: Seq[String]) {
  val key = dataSet.name + '_' + keyword
}

object ParsedQuery {
  val Sample = ParsedQuery(DataSet.Twitter,
    "rain",
    new Interval(new DateTime(2012, 1, 1, 0, 0).getMillis(), DateTime.now().getMillis),
    Seq("CA", "AZ", "NV"))
}

class CachesActor(val viewsActor: ActorRef) extends Actor with ActorLogging {
  def receive = {
    case q: ParsedQuery => {
      log.info("Caches:" + self + " get query from : " + sender())
      context.child(q.key).getOrElse {
        context.actorOf(Props(new CacheActor(viewsActor)(q.dataSet, q.keyword)), q.key)
      } forward q
    }
    case other =>
      log.info("Caches:" + self + "receive:" + other + " from : " + sender())
  }
}

