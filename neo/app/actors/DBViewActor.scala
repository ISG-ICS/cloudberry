package actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import db.{AQL, AQLConnection}
import models._
import org.joda.time.{DateTime, Interval}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json.JsArray

import scala.concurrent.Future

/**
  * View service is provided globally (across different node).
  * The original table is an special view
  * TODO think about the scalable case
  */
class DBViewActor(val dataSet: DataSet, val keyword: String, @volatile var curTimeRange: Interval = DBViewActor.DefaultInterval)
                 (implicit val conn: AQLConnection)
  extends Actor with ActorLogging with Stash {

  import DBViewActor._

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
        getTimeRangeDifference(viewMetaRecord.interval, Seq(curTimeRange)) match {
          case Some(missingIntervals) =>
            appendView(viewMetaRecord.interval, missingIntervals)
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
    case q: CacheQuery =>
      val optMissingInterval = getTimeRangeDifference(curTimeRange, Seq(q.timeRange))
      optMissingInterval match {
        case Some(interval) =>
          getTimeRangeDifference(toBeUpdatedTimeRange, interval).map { diffWithTobe: Seq[Interval] =>
            appendView(toBeUpdatedTimeRange, diffWithTobe).map {
              case newInterval: Interval =>
                updateViewMeta(newInterval)
                self ! UpdatedCurrentRange(newInterval)
            }
          }
          self forward q // Can not solve this query now.
        case None =>
          askViewAllInOne(q, sender())
      }
    case update: UpdatedCurrentRange =>
      curTimeRange = update.newInterval
  }

  def appendView(actual: Interval, missing: Seq[Interval]): Future[Interval] = {
    val futureInterval = new Interval(Math.min(actual.getStartMillis, missing.map(_.getStartMillis).reduceLeft(_ min _)),
                                      Math.max(actual.getEndMillis, missing.map(_.getEndMillis).reduceLeft(_ max _)))
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

  private def responseHandler(future: Future[Seq[QueryResult]], sender: ActorRef): Unit = {
    future onSuccess {
      case result: Seq[QueryResult] => {
        log.info("view send to cache:" + result)
        sender ! result
      }
      case _ =>
        log.info("ViewActor failed to wait for result")
    }

    future onFailure {
      case e => log.error(e, "askView Failed")
    }
  }

  private val keyMap = "map"
  private val keyTime = "time"
  private val keyHashtag = "hashtag"

  def askViewAllInOne(q: CacheQuery, sender: ActorRef): Unit = {
    val aql = AQL.aggregateByAllInOne(q)
    val f = conn.post(aql.statement).map { response =>
      log.info("dbQuery:" + response.json.toString())
      import QueryResult._
      Seq(keyMap, keyTime, keyHashtag).map { key =>
        QueryResult(key, q.dataSet.name, q.keyword, (response.json(0) \ key).as[Map[String, Int]])
      }
    }
    responseHandler(f, sender)
  }

  def askViewEach(q: CacheQuery, sender: ActorRef): Unit = {
    val fmap = dbQuery(q, keyMap)
    val ftime = dbQuery(q, keyTime)
    val ftag = dbQuery(q, keyHashtag)
    val all = for {
      mapResult <- fmap
      timeResult <- ftime
      hashTagResult <- ftag
    } yield (Seq[QueryResult](mapResult, timeResult, hashTagResult))
    responseHandler(all, sender)
  }

  def dbQuery(q: CacheQuery, aggType: String): Future[QueryResult] = {
    val aql = AQL.aggregateBy(q, aggType)

    conn.post(aql.statement).map { response =>
      log.info("dbQuery:" + response.json.toString())
      import QueryResult._
      QueryResult(aggType, q.dataSet.name, q.keyword, (response.json).as[Map[String, Int]])
    }
  }
}

object DBViewActor {
  val DefaultInterval = new Interval(new DateTime(2012, 1, 1, 0, 0).getMillis, DateTime.now().getMillis)

  object DoneInitializing

  case class UpdatedCurrentRange(newInterval: Interval)

  def getTimeRangeDifference(actual: Interval, expected: Seq[Interval]): Option[Seq[Interval]] = {
    Logger.logger.debug(s"actual: $actual, expected: $expected")
    if (expected.forall(actual.contains)) {
      None
    } else {
      // may need update query, but should not need to create query.
      import scala.collection.mutable.ArrayBuffer
      val futureInterval = new Interval(Math.min(actual.getStartMillis, expected.map(_.getStartMillis).reduceLeft(_ min _)),
                                        Math.max(actual.getEndMillis, expected.map(_.getEndMillis).reduceLeft(_ max _)))
      val intervals = ArrayBuffer.empty[Interval]
      if (futureInterval.getStartMillis < actual.getStartMillis) {
        intervals += new Interval(futureInterval.getStartMillis, actual.getStartMillis)
      }
      if (actual.getEndMillis < futureInterval.getEndMillis) {
        intervals += new Interval(actual.getEndMillis, futureInterval.getEndMillis)
      }
      Some(intervals)
    }
  }

}

/**
  * TODO you should also take care of the communicataion with DB. e.g. heartbeat check with db, failure recovery, etc.
  */
class DBViewsActor(implicit val aQLConnection: AQLConnection) extends Actor with ActorLogging {

  import scala.concurrent.duration._

  implicit val timeout = 5.seconds

  def receive = {
    case q: CacheQuery =>
      context.child(q.key).getOrElse {
        context.actorOf(Props(new DBViewActor(q.dataSet, q.keyword, superRange(q.timeRange, DBViewActor.DefaultInterval))),
                        q.key)
      } forward q
  }

  def superRange(interval1: Interval, interval2: Interval): Interval = {
    new Interval(Math.min(interval1.getStartMillis, interval2.getStartMillis),
                 Math.max(interval1.getEndMillis, interval2.getEndMillis))
  }
}

// This should be an remote service which will accept the update query for every different servers in the scale up case
object DBViewUpdater {

}
