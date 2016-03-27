package actors

import javax.inject.Singleton

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import models._
import org.joda.time.{DateTime, Interval}
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.JsArray

import scala.concurrent.Future

/**
  * View service is provided globally (across different node).
  * The original table is an special view
  * TODO think about the scalable case
  */
class ViewActor(val dataSet: DataSet, val keyword: String, @volatile var curTimeRange: Interval = ViewActor.DefaultInterval)
               (implicit val conn: AQLConnection)
  extends Actor with ActorLogging with Stash {

  import ViewActor._

  @volatile
  var toBeUpdatedTimeRange: Interval = _

  override def preStart(): Unit = {
    def findView(name: String, keyword: String): Future[Option[ViewMetaRecord]] = {
      conn.post(AQL.getView(dataSet.name, keyword)).map { respond =>
        log.info("findView: " + respond.body)
        val results = respond.json.as[JsArray]
        if (results.value.length == 0) {
          None
        } else {
          Some(results.value(0).as[ViewMetaRecord])
        }
      }
    }

    val result = findView(dataSet.name, keyword).flatMap {
      case Some(viewMetaRecord) => {
        getTimeRangeDifference(viewMetaRecord.interval, curTimeRange) match {
          case Some(missingInterval) =>
            appendView(viewMetaRecord.interval, missingInterval)
          case None =>
            Future {
              curTimeRange = viewMetaRecord.interval
            }
        }
      }
      case None => {
        createView(curTimeRange)
      }
    }

    result onSuccess {
      case span: Interval => {
        curTimeRange = span
        updateViewMeta(span)
        self ! DoneInitializing
      }
    }

    result onFailure {
      case e: Throwable => {
        log.error(e, "view actor initializing failed")
        throw e
      }
    }
  }

  def receive = initializing

  def initializing: Receive = {
    case DoneInitializing =>
      unstashAll()
      context.become(initialized)
    case msg => stash()
  }

  def initialized: Receive = {
    case q: ParsedQuery =>
      val optMissingInterval = getTimeRangeDifference(curTimeRange, q.timeRange)
      optMissingInterval match {
        case Some(interval) =>
          getTimeRangeDifference(toBeUpdatedTimeRange, interval).map { diffWithTobe: Interval =>
            appendView(toBeUpdatedTimeRange, diffWithTobe).map {
              case newInterval: Interval =>
                updateViewMeta(newInterval)
                self ! UpdatedCurrentRange(newInterval)
            }
          }
          self forward q // Can not solve this query now.
        case None =>
          askView(q, sender())
      }
    case update: UpdatedCurrentRange =>
      curTimeRange = update.newInterval
  }

  def appendView(actual: Interval, missing: Interval): Future[Interval] = {
    val futureInterval = new Interval(Math.min(actual.getStartMillis, missing.getStartMillis),
      Math.max(actual.getEndMillis, missing.getEndMillis))
    toBeUpdatedTimeRange = futureInterval
    conn.post(AQL.appendView(dataSet, keyword, missing)).map {
      case respond => {
        log.info("append view:" + respond.body)
        futureInterval
      }
    }
  }

  def createView(initialTimeSpan: Interval): Future[Interval] = {
    conn.post(AQL.createView(dataSet, keyword, initialTimeSpan)) map {
      case respond => {
        log.info("create view:" + respond.body)
        initialTimeSpan
      }
    }
  }

  def updateViewMeta(newInterval: Interval): Unit = {
    conn.post(AQL.updateViewMeta(dataSet.name, keyword, newInterval).statement)
  }

  def askView(q: ParsedQuery, sender: ActorRef): Unit = {
    (dbQuery(AQL.translateQueryToAQL(q))).onSuccess {
      case viewResult => {
        sender ! viewResult
      }
    }
  }

  def dbQuery(aql: AQL): Future[QueryResult] = {
    conn.post(aql.statement).map { response =>
      log.info("dbQuery:" + response.json.toString())
      import QueryResult._
      QueryResult(1, (response.json).as[Map[String, Int]])
    }
  }
}

object ViewActor {
  val DefaultInterval = new Interval(new DateTime(2012, 1, 1, 0, 0).getMillis, DateTime.now().getMillis)

  object DoneInitializing

  case class UpdatedCurrentRange(newInterval: Interval)

  def getTimeRangeDifference(actual: Interval, expected: Interval): Option[Interval] = {
    if (actual.contains(expected)) {
      None
    } else {
      // may need update query, but should not need to create query.
      Some(
        if (actual.getStartMillis < expected.getStartMillis) (new Interval(actual.getEnd, expected.getEnd))
        else (new Interval(expected.getStart, actual.getStart))
      )
    }
  }

}

@Singleton
class ViewsActor(implicit val aQLConnection: AQLConnection) extends Actor with ActorLogging {

  import scala.concurrent.duration._

  implicit val timeout = 5.seconds

  def receive = {
    case q: ParsedQuery =>
      context.child(q.key).getOrElse {
        context.actorOf(Props(new ViewActor(q.dataSet, q.keyword, superRange(q.timeRange, ViewActor.DefaultInterval))),
          q.key)
      } forward q
  }

  def superRange(interval1: Interval, interval2: Interval): Interval = {
    new Interval(Math.min(interval1.getStartMillis, interval2.getStartMillis),
      Math.max(interval1.getEndMillis, interval2.getEndMillis))
  }
}

// This should be an remote service which will accept the update query for every different servers in the scale up case
object ViewUpdater {

}
