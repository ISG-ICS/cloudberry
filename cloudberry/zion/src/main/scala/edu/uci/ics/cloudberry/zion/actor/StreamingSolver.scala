package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Stash}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.TInterval
import edu.uci.ics.cloudberry.zion.actor.DataStoreManager.AskInfoAndViews
import edu.uci.ics.cloudberry.zion.actor.StreamingSolver.{PartialResult, QueryGroup, QueryInfo, SlicingRequest}
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.IPostTransform
import edu.uci.ics.cloudberry.zion.model.impl.QueryPlanner.IMerger
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, QueryPlanner}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.DateTime
import play.api.libs.json.JsArray

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class StreamingSolver(val dataManager: ActorRef,
                      val planner: QueryPlanner,
                      val config: Config,
                      val out: ActorRef
                     )(implicit val ec: ExecutionContext,
                       implicit val materializer: Materializer) extends Actor with Stash with IQuerySolver with ActorLogging {

  implicit val askTimeOut: Timeout = config.UserTimeOut
  private val minTimeGap = config.MinTimeGap



  override def receive: Receive = {
    case request : SlicingRequest =>
      val queryInfos = request.queries.map { query =>
        val info = request.infos(query.dataset)
        if (!info.schema.isInstanceOf[Schema]) {
          throw new IllegalArgumentException("Initial schema " + info.schema.getTypeName + " is a Lookup Schema")
        }
        val schema = info.schema.asInstanceOf[Schema]
        val bound = query.getTimeInterval(schema.timeField).getOrElse(new TInterval(info.dataInterval.getStart, DateTime.now))
        val merger = planner.calculateMergeFunc(query, info.schema)
        val queryWOTime = query.copy(filter = query.filter.filterNot(_.field == schema.timeField))
        QueryInfo(queryWOTime, info, bound, merger)
      }
      val min = queryInfos.map(_.queryBound.getStartMillis).min
      val max = queryInfos.map(_.queryBound.getEndMillis).max
      val boundary = new TInterval(min, max)

      val initialDuration = config.FirstQueryTimeGap.toMillis
      val interval = calculateFirst(boundary, initialDuration)
      val queryGroup = QueryGroup(queryInfos, request.postTransform)
      val initResult = Seq.fill(queryInfos.size)(JsArray())
      issueQueryGroup(interval, queryGroup)
      context.become(askSlice(request.targetMillis, interval, boundary, queryGroup, initResult, askTime = DateTime.now), discardOld = true)
  }

  private def askSlice(targetInvertal: Long,
                       curInterval: TInterval,
                       boundary: TInterval,
                       queryGroup: QueryGroup,
                       accumulateResults: Seq[JsArray],
                       askTime: DateTime): Receive = {
    case result: PartialResult =>
      val mergedResults = queryGroup.queries.zipWithIndex.map { case (q, idx) =>
        q.merger(Seq(accumulateResults(idx), result.jsons(idx)))
      }
      out ! queryGroup.postTransform.transform(JsArray(mergedResults))

      val timeSpend = DateTime.now.getMillis - askTime.getMillis
      val nextInterval = calculateNext(targetInvertal, curInterval, timeSpend, boundary)
      if (nextInterval.toDurationMillis == 0) {
        out ! queryGroup.postTransform.transform(BerryClient.Done) // notifying the client the processing is done
        suggestViews(queryGroup)
        context.become(receive, discardOld = true)
      } else {
        issueQueryGroup(nextInterval, queryGroup)
        context.become(askSlice(targetInvertal, nextInterval, boundary, queryGroup, mergedResults, DateTime.now), discardOld = true)
      }
  }

  private def suggestViews(queryGroup: QueryGroup): Unit = {
    for (queryInfo <- queryGroup.queries) {
      dataManager ? AskInfoAndViews(queryInfo.query.dataset) map {
        case seq: Seq[_] if seq.forall(_.isInstanceOf[DataSetInfo]) =>
          val infos = seq.map(_.asInstanceOf[DataSetInfo])
          val newViews = planner.suggestNewView(queryInfo.query, infos.head, infos.tail)
          newViews.foreach(dataManager ! _)
      }
    }
  }

  private def issueQueryGroup(interval: TInterval, queryGroup: QueryGroup): Unit = {
    val futures = Future.traverse(queryGroup.queries) { queryInfo =>
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
        self ! PartialResult(queryGroup, answers.map(_.asInstanceOf[JsArray]))
      case Failure(fails) => log.error(fails, "answer query failed")
    }
  }

  private def calculateFirst(entireInterval: TInterval, gap: Long): TInterval = {
    val startTime = Math.max(entireInterval.getEndMillis - gap, entireInterval.getStartMillis)
    new TInterval(startTime, entireInterval.getEndMillis)
  }

  private def calculateNext(targetTimeSpend: Long, interval: TInterval, timeSpend: Long, boundary: TInterval): TInterval = {
    val newDuration = Math.max(minTimeGap.toMillis, (interval.toDurationMillis * targetTimeSpend / timeSpend.toDouble).toLong)
    val startTime = Math.max(boundary.getStartMillis, interval.getStartMillis - newDuration)
    new TInterval(startTime, interval.getStartMillis)
  }


}

object StreamingSolver{
  case class SlicingRequest(targetMillis: Long, queries: Seq[Query], infos: Map[String, DataSetInfo], postTransform: IPostTransform)

  private case class PartialResult(queryGroup: QueryGroup, jsons: Seq[JsArray])

  private case class QueryInfo(query: Query, dataSetInfo: DataSetInfo, queryBound: TInterval, merger: IMerger)

  private case class QueryGroup(queries: Seq[QueryInfo], postTransform: IPostTransform)

}
