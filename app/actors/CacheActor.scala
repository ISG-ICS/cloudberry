package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import models.QueryResult
import org.joda.time.Interval
import play.api.libs.concurrent.Execution.Implicits._
import play.libs.Akka

/**
  * There is one cache per keyword
  */
class CacheActor(val keyword: String) extends Actor with ActorLogging {
  @volatile
  var timeRange: Interval = Interval.parse("")

  def answerAsMuchAsICan(q: ParsedQuery): QueryResult = ???

  def splitQuery(q: ParsedQuery): Option[ParsedQuery] = ???

  def answerFromView(viewQuery: ParsedQuery, cachedAnswer: QueryResult, sender: ActorRef) = {

    import akka.pattern.ask

    import scala.concurrent.duration._
    implicit val timeout = Timeout(5.seconds)

    (ViewsActor.viewsActor ? viewQuery).mapTo[QueryResult] onSuccess {
      case viewAnswer => {
        updateCache(viewQuery, viewAnswer)
        sender ! (cachedAnswer + viewAnswer)
      }
    }
  }

  def updateCache(q: ParsedQuery, viewAnswer: QueryResult) = {
    ???
    timeRange = q.timeRange
  }

  def receive = {
    case q: ParsedQuery =>
      val cachedAnswer: QueryResult = answerAsMuchAsICan(q)
      splitQuery(q) match {
        case Some(viewQuery) =>
          answerFromView(viewQuery, cachedAnswer, sender)
        case None =>
          sender ! cachedAnswer
      }
    case q: Any =>
      log.info(s"cache $keyword got unknown msg + $q")
  }
}

class CachesActor extends Actor with ActorLogging {

  def receive = {
    case q: ParsedQuery => {
      context.child(q.keyword).getOrElse {
        context.actorOf(Props(new CacheActor(q.keyword)), q.keyword)
      } forward q
    }
    case q: Any => {
      log.info("cached received:" + q)
    }
  }
}

object CachesActor {
  lazy val cachesActor: ActorRef = Akka.system.actorOf(Props(classOf[CachesActor]))
}