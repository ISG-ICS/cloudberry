package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import models.QueryResult
import play.api.libs.concurrent.Execution.Implicits._
import play.libs.Akka

/**
  * View service is provided globally (across different node).
  * The original table is an special view
  */
class ViewActor(val keyword: String) extends Actor with ActorLogging {
  def answerAsMuchAsICan(q: Any): QueryResult = ???

  def splitQuery(q: Any): Option[Any] = ???

  def updateView(dbQuery: Any) = {
    //send the m
  }

  def answerFromDB(dbQuery: Any, cachedAnswer: QueryResult, sender: ActorRef): Unit = {
    import akka.pattern.ask

    import scala.concurrent.duration._
    implicit val timeout = Timeout(5.seconds)

    (ViewsActor.viewsActor ? dbQuery).mapTo[QueryResult] onSuccess {
      case viewAnswer => {
        sender ! (cachedAnswer + viewAnswer)
        updateView(dbQuery)
      }
    }
  }

  def receive = {
    case q: ParsedQuery =>
      val cachedAnswer: QueryResult = answerAsMuchAsICan(q)
      splitQuery(q) match {
        case Some(dbQuery) =>
          answerFromDB(dbQuery, cachedAnswer, sender)
        case None =>
          sender ! cachedAnswer
      }
  }
}

class ViewsActor extends Actor with ActorLogging {

  import scala.concurrent.duration._

  implicit val timeout = 5.seconds

  def receive = {
    case q: ParsedQuery =>
      context.child(q.keyword).getOrElse {
        context.actorOf(Props(new ViewActor(q.keyword)), q.keyword)
      } forward q
  }

}

object ViewsActor {
  lazy val viewsActor: ActorRef = Akka.system.actorOf(Props(classOf[ViewsActor]))
}

// This should be an remote service which will accept the update query for every different servers
object ViewUpdater {

}
