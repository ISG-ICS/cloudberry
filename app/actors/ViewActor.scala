package actors

import javax.inject.Singleton

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import models.{AQL, DataSet, QueryResult}
import org.joda.time.{DateTime, Interval}
import play.api.Configuration
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.Future

/**
  * View service is provided globally (across different node).
  * The original table is an special view
  * TODO think about the scalable case
  */
class ViewActor(val dataSet: DataSet, val keyword: String, var timeRange: Interval = ViewActor.DefaultInterval)
               (implicit val ws: WSClient, implicit val config: Configuration)
  extends Actor with ActorLogging with Stash{

  import ViewActor._

  val AsterixURL = config.getString(ConfigKeyAsterixURL).get

  object DonePrepare

  override def preStart(): Unit = {
    // check if table exist by asking the views meta table.
    // create if not exist
    // append if not covered
    ws.url(AsterixURL).post(AQL.lookupView)
    ws.url(AsterixURL).post(AQL.updateView(dataSet, dataSet.name + keyword, keyword, timeRange).statement) onSuccess {
      case respond : WSResponse => self ! DonePrepare
    }
  }

  def preparing : Receive = {
    case DonePrepare =>
      unstashAll()
      context.become(prepared)
    case msg => stash()
  }

  def prepared : Receive = {
    case q: ParsedQuery =>
      val (optAppendAQL, aggrAQL, optUpdateMetaAQL, newInterval) = parseToAQL(q)

      optAppendAQL match {
        case Some(aql: AQL) =>
          updateViewAndThenSearch(aql, aggrAQL, optUpdateMetaAQL.get, newInterval.get, sender())
        case None =>
          askView(aggrAQL, sender())
      }
    case update: UpdateMsg =>
      timeRange = update.newInterval
  }

  def receive = preparing

  def updateViewAndThenSearch(appendAQL: AQL, aggrAQL: AQL, updateMetaAQL: AQL, newInterval: Interval, sender: ActorRef) = {
    dbManipulate(appendAQL) andThen {
      case wsRespond: WSResponse => {
        log.info(wsRespond.body)
        askView(aggrAQL, sender)
        updateMeta(updateMetaAQL, newInterval)
      }
    }
  }

  def updateMeta(updateMetaAQL: AQL, newInterval: Interval): Unit = {
    (dbManipulate(updateMetaAQL)) onSuccess {
      case wsRespond: WSResponse => {
        log.info(wsRespond.body)
        self ! UpdateMsg(newInterval)
      }
    }
  }

  def askView(aggrAQL: AQL, sender: ActorRef): Unit = {
    (dbQuery(aggrAQL)).onSuccess {
      case viewResult => {
        sender ! viewResult
      }
    }
  }

  def parseToAQL(q: ParsedQuery): (Option[AQL], AQL, Option[AQL], Option[Interval]) = {
    // may need update query, but should not need to create query.
    (None, AQL.translateQueryToAQL(q), None, None)
  }

  def dbManipulate(aql: AQL): Future[WSResponse] = {
    ws.url(AsterixURL).post(aql.statement)
  }

  def dbQuery(aql: AQL): Future[QueryResult] = {
    ws.url(AsterixURL).post(aql.statement).map { response =>
      log.info(response.json.toString())
      import QueryResult._
      QueryResult(1, (response.json).as[Map[String, Int]])
    }
  }
}

object ViewActor {
  val ConfigKeyAsterixURL = "asterixdb.url"

  val DefaultInterval = new Interval(new DateTime(2012, 1, 1, 0, 0).getMillis, DateTime.now().getMillis)

  case class UpdateMsg(newInterval: Interval)

}

@Singleton
class ViewsActor(implicit val ws: WSClient, implicit val config: Configuration) extends Actor with ActorLogging with Stash {

  import scala.concurrent.duration._

  implicit val timeout = 5.seconds

  val AsterixURL = config.getString(ViewActor.ConfigKeyAsterixURL).get

  object Done

  override def preStart(): Unit = {
    ws.url(AsterixURL).post(AQL.createViewMetaTable.statement) onSuccess {
      case wsRespond: WSResponse =>
        log.info(wsRespond.body)
        self ! Done
    }
  }

  def initializing: Receive = {
    case Done =>
      unstashAll()
      context.become(initialized)
    case _ => stash()
  }

  def initialized: Receive = {
    case q: ParsedQuery =>
      context.child(q.key).getOrElse {
        context.actorOf(Props(new ViewActor(q.dataSet, q.keyword, superRange(q.timeRange, ViewActor.DefaultInterval))),
          q.key)
      } forward q
  }

  def receive = initializing

  def superRange(interval1: Interval, interval2: Interval): Interval = {
    new Interval(Math.min(interval1.getStartMillis, interval2.getStartMillis),
      Math.max(interval1.getEndMillis, interval2.getEndMillis))
  }
}

// This should be an remote service which will accept the update query for every different servers
object ViewUpdater {

}
