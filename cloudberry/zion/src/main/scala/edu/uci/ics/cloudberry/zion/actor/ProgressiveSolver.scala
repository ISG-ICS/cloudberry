package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Stash}
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.TInterval
import edu.uci.ics.cloudberry.zion.actor.ProgressiveSolver._
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.IPostTransform
import edu.uci.ics.cloudberry.zion.model.impl.QueryPlanner.IMerger
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, QueryPlanner}
import edu.uci.ics.cloudberry.zion.model.schema._
import edu.uci.ics.cloudberry.zion.model.slicing.Drum
import org.joda.time.DateTime
import play.api.libs.json.{JsArray, JsNumber}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * The Streaming Actor slices one query to a sequence of mini-queries and responds with a stream of partial results.
  * If it receives multiple queries, the later queries will be stashed until the existing slicing is over.
  * If it receives a Cancel message, the current slicing will be stopped.
  *
  * @param dataManager
  * @param planner
  * @param config
  * @param out
  */
class ProgressiveSolver(val dataManager: ActorRef,
                        val planner: QueryPlanner,
                        val config: Config,
                        val out: ActorRef
                       )(implicit val ec: ExecutionContext) extends Actor with Stash with IQuerySolver with ActorLogging {

  implicit val askTimeOut: Timeout = config.UserTimeOut

  private var ts: Long = 0
  private val reporter: ActorRef = context.actorOf(Props(new Reporter(out)))

  override def receive: Receive = {
    case request: SlicingRequest =>
      ts = DateTime.now().getMillis
      reporter ! Reporter.Reset(FiniteDuration(request.intervalMS, "ms"))
      val queryInfos = request.queries.map { query =>
        val info = request.infos(query.dataset)
        if (!info.schema.isInstanceOf[Schema]) {
          throw new IllegalArgumentException("Initial schema " + info.schema.getTypeName + " is a Lookup Schema")
        }
        val schema = info.schema.asInstanceOf[Schema]
        val bound = query.getTimeInterval(schema.timeField).getOrElse(new TInterval(info.dataInterval.getStart, DateTime.now))
        val merger = planner.calculateMergeFunc(query, info.schema)
        val queryWOTime = query.copy(filter = query.filter.filterNot(_.field == schema.timeField))
        MiniQuery(queryWOTime, info, bound, merger)
      }
      val min = queryInfos.map(_.queryBound.getStartMillis).min
      val max = queryInfos.map(_.queryBound.getEndMillis).max
      val boundary = new TInterval(min, max)

      val initialDuration = config.FirstQueryTimeGap
      val interval = calculateFirst(boundary, initialDuration)
      val queryGroup = QueryGroup(ts, queryInfos, request.postTransform)
      val initResult = Seq.fill(queryInfos.size)(JsArray())
      issueQueryGroup(interval, queryGroup)
      val drumEstimator = new Drum(boundary.toDuration.getStandardHours.toInt, alpha = 1, initialDuration.toHours.toInt)
      context.become(askSlice(request.resultSizeLimitOpt, request.intervalMS, request.intervalMS, interval, drumEstimator, Int.MaxValue, boundary, queryGroup, initResult, issuedTimestamp = DateTime.now), discardOld = true)
    case _: MiniQueryResult =>
      // do nothing
      log.debug(s"receive: obsolete query result")
    case Cancel =>
      // do nothing
      log.debug(s"receive: cancel")
  }

  private def askSlice(resultSizeLimitOpt: Option[Int],
                       paceMS: Long,
                       timeLimitMS: Long,
                       curInterval: TInterval,
                       estimator: Drum,
                       curEstimateMS: Long,
                       boundary: TInterval,
                       queryGroup: QueryGroup,
                       accumulateResults: Seq[JsArray],
                       issuedTimestamp: DateTime): Receive = {
    case result: MiniQueryResult if result.key == ts =>
      val mergedResults = queryGroup.queries.zipWithIndex.map {
        case (q, idx) =>
          q.merger(Seq(accumulateResults(idx), result.jsons(idx)))
      }

      val timeSpend = DateTime.now.getMillis - issuedTimestamp.getMillis
      val diff = Math.max(0, timeLimitMS - timeSpend)
      val nextLimit = paceMS + diff

      val (nextInterval, nextEstimateMS) = calculateNext(estimator, nextLimit, curInterval, curEstimateMS, timeSpend, boundary)
      if (nextInterval.toDurationMillis == 0 || hasEnoughResults(mergedResults, resultSizeLimitOpt)) { //finished slicing

        val limitResultOpt = resultSizeLimitOpt.map(limit => Seq(JsArray(mergedResults.head.value.take(limit))))
        val returnedResult = limitResultOpt.getOrElse(mergedResults)
        reporter ! Reporter.PartialResult(curInterval.getStartMillis, boundary.getEndMillis, 1.0, queryGroup.postTransform.transform(JsArray(returnedResult)))
        reporter ! Reporter.Fin(queryGroup.postTransform.transform(BerryClient.Done))

        queryGroup.queries.foreach(qinfo => suggestViews(qinfo.query))
        unstashAll() // in case there are new queries
        context.become(receive, discardOld = true)
      } else {
        val progress = if (resultSizeLimitOpt.isDefined) {
          mergedResults.size / resultSizeLimitOpt.get.toDouble
        } else {
          curInterval.withEnd(boundary.getEnd).toDurationMillis.toDouble / boundary.toDurationMillis
        }
        reporter ! Reporter.PartialResult(curInterval.getStartMillis, boundary.getEndMillis, progress, queryGroup.postTransform.transform(JsArray(mergedResults)))
        issueQueryGroup(nextInterval, queryGroup)
        context.become(askSlice(resultSizeLimitOpt, paceMS, nextLimit, nextInterval, estimator, nextEstimateMS, boundary, queryGroup, mergedResults, DateTime.now), discardOld = true)
      }
    case result: MiniQueryResult =>
      log.debug(s"old result: $result")
    case _: SlicingRequest =>
      stash()
    case ProgressiveSolver.Cancel =>
      reporter ! Reporter.Fin(JsNumber(0))
      log.debug("askslice resceive cancel")
      unstashAll()
      context.become(receive, discardOld = true)
  }

  private def issueQueryGroup(interval: TInterval, queryGroup: QueryGroup): Unit = {
    val futures = Future.traverse(queryGroup.queries) {
      queryInfo =>
        if (queryInfo.queryBound.overlaps(interval)) {
          val overlaps = queryInfo.queryBound.overlap(interval)
          val schema = queryInfo.dataSetInfo.schema.asInstanceOf[Schema]
          val timeFilter = FilterStatement(schema.timeField, None, Relation.inRange,
            Seq(overlaps.getStart, overlaps.getEnd).map(TimeField.TimeFormat.print))
          solveAQuery(queryInfo.query.copy(filter = timeFilter +: queryInfo.query.filter))
        } else {
          Future(JsArray())
        }
    }

    futures.onComplete {
      case Success(answers) =>
        self ! MiniQueryResult(queryGroup.key, queryGroup, answers.map(_.asInstanceOf[JsArray]))
      case Failure(fails) => log.error(fails, "answer query failed")
    }
  }

  private def calculateFirst(entireInterval: TInterval, duration: FiniteDuration): TInterval = {
    val startTime = Math.max(entireInterval.getEndMillis - duration.toMillis, entireInterval.getStartMillis)
    new TInterval(startTime, entireInterval.getEndMillis)
  }

  private def calculateNext(drum: Drum, timeLimit: Long, lastInterval: TInterval, lastEstimateMS: Long, lastActualMS: Long, boundary: TInterval): (TInterval, Long) = {
    drum.learn(lastInterval.toDuration.getStandardHours.toInt, lastEstimateMS.toInt, lastActualMS.toInt)
    val estimate = drum.estimate(timeLimit.toInt)

    val startTime = Math.max(boundary.getStartMillis, lastInterval.getStart.minusHours(estimate.range).getMillis)
    val interval = new TInterval(startTime, lastInterval.getStartMillis)
    (interval, estimate.estimateMS.toLong)
  }

  private def hasEnoughResults(results: Seq[JsArray], resultLimitOpt: Option[Int]): Boolean = {
    resultLimitOpt.nonEmpty && results.nonEmpty && results.head.value.size >= resultLimitOpt.get
  }

}

object ProgressiveSolver {

  case object Cancel

  case class SlicingRequest(intervalMS: Long, resultSizeLimitOpt: Option[Int], queries: Seq[Query], infos: Map[String, DataSetInfo], postTransform: IPostTransform)

  private case class MiniQueryResult(key: Long, queryGroup: QueryGroup, jsons: Seq[JsArray])

  private case class MiniQuery(query: Query, dataSetInfo: DataSetInfo, queryBound: TInterval, merger: IMerger)

  private case class QueryGroup(key: Long, queries: Seq[MiniQuery], postTransform: IPostTransform)

}
