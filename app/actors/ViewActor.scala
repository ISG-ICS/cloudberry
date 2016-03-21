package actors

import javax.inject.Singleton

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import models.{AQL, QueryResult}
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WSClient

import scala.concurrent.Future

/**
  * View service is provided globally (across different node).
  * The original table is an special view
  */
class ViewActor(val keyword: String)(implicit val ws: WSClient,
                                     implicit val config: Configuration) extends Actor with ActorLogging {

  import ViewActor._

  def prepareAQLForViewAndDB(q: ParsedQuery): (AQL, Option[AQL]) = {
    (AQL.translateQueryToAQL(q), None)
  }

  def upsertView(dbQuery: AQL) = {
    //send the m
  }

  def dbQuery(aql: AQL): Future[QueryResult] = {
    ws.url(config.getString(AsterixURL).get).post(aql.statement).map { response =>
      log.info(response.body)
      QueryResult.SampleView
    }
  }

  def mergeAnswerFromDB(viewAQL: AQL, dbAQL: Option[AQL], sender: ActorRef): Unit = {

    import scala.concurrent.duration._
    implicit val timeout = Timeout(5.seconds)

    val aggResults = for {
      viewResult <- dbQuery(viewAQL)
      dbResult <- dbAQL match {
        case Some(query) => dbQuery(query)
        case None => Future {
          QueryResult.Empty
        }
      }
    } yield (viewResult, dbResult)

    aggResults.onSuccess {
      case (viewResult, dbResult) => {
        val merged = viewResult + dbResult
        sender ! merged
        dbAQL.map(upsertView)
      }
    }
  }

  def receive = {
    case q: ParsedQuery =>
      val (viewAql, dbAQL) = prepareAQLForViewAndDB(q)
      mergeAnswerFromDB(viewAql, dbAQL, sender)
  }
}

object ViewActor {
  val AsterixURL = "asterixdb.url"
}

@Singleton
class ViewsActor(implicit val ws: WSClient, implicit val config: Configuration) extends Actor with ActorLogging {

  import scala.concurrent.duration._

  implicit val timeout = 5.seconds

  def receive = {
    case q: ParsedQuery =>
      context.child(q.keyword).getOrElse {
        context.actorOf(Props(new ViewActor(q.keyword)), q.keyword)
      } forward q
  }

}

// This should be an remote service which will accept the update query for every different servers
object ViewUpdater {

}
