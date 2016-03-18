package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import models.QueryResult
import play.api.libs.concurrent.Execution.Implicits._
import play.libs.Akka

import scala.concurrent.Future

/**
  * View service is provided globally (across different node).
  * The original table is an special view
  */
class ViewActor(val keyword: String) extends Actor with ActorLogging {

  def splitQuery(q: Any): (Any, Any) = (ParsedQuery.Sample, ParsedQuery.Sample)

  def updateView(dbQuery: Any) = {
    //send the m
  }

  def dbQuery(aql: Any): Future[QueryResult] = Future {
    QueryResult.SampleView
  }

  def mergeAnswerFromDB(viewAQL: Any, dbAQL: Any, sender: ActorRef): Unit = {

    import scala.concurrent.duration._
    implicit val timeout = Timeout(5.seconds)

    val aggResults = for {
      viewResult <- dbQuery(viewAQL)
      dbResult <- dbQuery(dbAQL)
    } yield (viewResult, dbResult)

    aggResults.onSuccess {
      case (viewResult, dbResult) => {
        val merged = viewResult + dbResult
        sender ! merged
        updateView(dbAQL)
      }
    }
  }

  def receive = {
    case q: ParsedQuery =>
      val (viewAql, dbAQL) = splitQuery(q)
      mergeAnswerFromDB(viewAql, dbAQL, sender)
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
  lazy val viewsActor: ActorRef = Akka.system.actorOf(Props(classOf[ViewsActor]), "views")
}

// This should be an remote service which will accept the update query for every different servers
object ViewUpdater {

}
