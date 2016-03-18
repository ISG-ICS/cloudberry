package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import models.QueryResult
import org.joda.time.{DateTime, Interval}
import play.api.libs.concurrent.Execution.Implicits._
import play.libs.Akka

/**
  * There is one cache per keyword
  */
class CacheActor(val keyword: String) extends Actor with ActorLogging {

  @volatile
  var timeRange: Interval = new Interval(new DateTime(2012, 1, 1, 0, 0).getMillis, DateTime.now().getMillis)

  def answerAsMuchAsICan(q: ParsedQuery): QueryResult = {
    QueryResult.SampleCache
  }

  def splitQuery(q: ParsedQuery): Option[ParsedQuery] = Some(q)

  def mergeAnswerFromView(viewQuery: ParsedQuery, cachedAnswer: QueryResult, sender: ActorRef) = {

    import akka.pattern.ask

    import scala.concurrent.duration._
    implicit val timeout = Timeout(5.seconds)

    (ViewsActor.viewsActor ? viewQuery).mapTo[QueryResult] onSuccess {
      case viewAnswer => {
        sender ! (cachedAnswer + viewAnswer)
        updateCache(viewQuery, viewAnswer)
      }
    }
  }

  def updateCache(q: ParsedQuery, viewAnswer: QueryResult) = {
    timeRange = q.timeRange
  }

  def receive = {
    case q: ParsedQuery =>
      val cachedAnswer: QueryResult = answerAsMuchAsICan(q)
      splitQuery(q) match {
        case Some(viewQuery) =>
          mergeAnswerFromView(viewQuery, cachedAnswer, sender)
        case None =>
          sender ! cachedAnswer
      }
  }
}

class CachesActor extends Actor with ActorLogging {

  def receive = {
    case q: ParsedQuery => {
      context.child(q.keyword).getOrElse {
        context.actorOf(Props(new CacheActor(q.keyword)), q.keyword)
      } forward q
    }
  }
}

object CachesActor {
  lazy val cachesActor: ActorRef = Akka.system.actorOf(Props(classOf[CachesActor]), "caches")
}