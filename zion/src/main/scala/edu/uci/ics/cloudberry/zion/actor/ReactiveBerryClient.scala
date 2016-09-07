package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, Props, Stash}
import akka.pattern.ask
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.TInterval
import edu.uci.ics.cloudberry.zion.actor.DataStoreManager.{AskInfo, AskInfoAndViews}
import edu.uci.ics.cloudberry.zion.actor.RESTFulBerryClient.NoSuchDataset
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.impl.QueryPlanner.IMerger
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, JSONParser, QueryPlanner}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.DateTime
import play.api.libs.json.{JsArray, JsValue}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * A reactive client which will continuously feed the result back to user
  * One user should only attach to one ReactiveClient
  */
class ReactiveBerryClient(val jsonParser: JSONParser,
                          val dataManager: ActorRef,
                          val planner: QueryPlanner,
                          val config: Config,
                          val responseTime: Long,
                          childMaker: (ActorRefFactory, ReactiveBerryClient) => ActorRef
                         )
                         (implicit ec: ExecutionContext) extends Actor with Stash with ActorLogging {

  import ReactiveBerryClient._

  implicit val askTimeOut: Timeout = config.UserTimeOut

  val worker = childMaker(context, this)

  private case class QueryInfo(query: Query, dataSetInfo: DataSetInfo, postTransform: IPostTransform, queryBound: TInterval, merger: IMerger)

  private case class QueryGroup(ts: DateTime, curSender: ActorRef, queries: Seq[QueryInfo])

  private case class Initial(ts: DateTime, sender: ActorRef, queries: Seq[Query], infos: Seq[DataSetInfo], postTransforms: Seq[IPostTransform])

  private case class PartialResult(queryGroup: QueryGroup, jsons: Seq[JsArray])

  //WARNING: Evil mutable value SHALL NOT be read/write in the future thread
  var curKey: DateTime = _

  override def receive: Receive = {
    case request: Request =>
      val queries = request.requests.map(r => jsonParser.parse(r._1))
      val curSender = sender()
      val key = DateTime.now()
      curKey = key
      val fDataInfos = Future.traverse(queries) { query =>
        dataManager ? AskInfo(query.dataset)
      }.map(seq => seq.map(_.asInstanceOf[Option[DataSetInfo]]))

      fDataInfos.map { seqInfos =>
        if (seqInfos.exists(_.isEmpty)) {
          curSender ! NoSuchDataset(queries(seqInfos.indexOf(None)).dataset)
        } else {
          self ! Initial(key, curSender, queries, seqInfos.map(_.get), request.requests.map(_._2))
        }
      }
    case initial: Initial if initial.ts == curKey =>
      val queryInfos = initial.queries.zip(initial.infos).zip(initial.postTransforms).map {
        case ((query, info), trans) =>
          val bound = query.getTimeInterval(info.schema.timeField).getOrElse(new TInterval(info.dataInterval.getStart, DateTime.now))
          val merger = planner.calculateMergeFunc(query, info.schema)
          val queryWOTime = query.copy(filter = query.filter.filterNot(_.fieldName == info.schema.timeField))
          QueryInfo(queryWOTime, info, trans, bound, merger)
      }
      val min = queryInfos.map(_.queryBound.getStartMillis).min
      val max = queryInfos.map(_.queryBound.getEndMillis).max
      val boundary = new TInterval(min, max)

      val initialDuration = config.FirstQueryTimeGap.toMillis
      val interval = calculateFirst(boundary, initialDuration)
      val queryGroup = QueryGroup(initial.ts, initial.sender, queryInfos)
      val initResult = Seq.fill(queryInfos.size)(JsArray())
      issueAQuery(interval, queryGroup)
      context.become(askSlice(interval, boundary, queryGroup, initResult, askTime = DateTime.now), discardOld = true)
  }

  private def askSlice(curInterval: TInterval,
                       boundary: TInterval,
                       queryGroup: QueryGroup,
                       accumulateResults: Seq[JsArray],
                       askTime: DateTime): Receive = {
    case result: PartialResult if result.queryGroup.ts == curKey =>
      val mergedResults = queryGroup.queries.zipWithIndex.map { case (q, idx) =>
        q.merger(Seq(accumulateResults(idx), result.jsons(idx)))
      }
      //TODO merge it as an array
      mergedResults.zipWithIndex.foreach { case (ret, idx) =>
        queryGroup.curSender ! queryGroup.queries(idx).postTransform.transform(ret)
      }

      val timeSpend = DateTime.now.getMillis - askTime.getMillis
      val nextInterval = calculateNext(curInterval, timeSpend, boundary)
      if (nextInterval.toDurationMillis == 0) {
        suggestViews(queryGroup)
        context.become(receive, discardOld = true)
      } else {
        issueAQuery(nextInterval, queryGroup)
        context.become(askSlice(nextInterval, boundary, queryGroup, mergedResults, DateTime.now), discardOld = true)
      }
    case newRequest: Request =>
      stash()
      unstashAll()
      context.become(receive, discardOld = true)
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

  //Main thread
  private def issueAQuery(interval: TInterval, queryGroup: QueryGroup): Unit = {
    val futures = Future.traverse(queryGroup.queries) { queryInfo =>
      if (queryInfo.queryBound.overlaps(interval)) {
        val overlaps = queryInfo.queryBound.overlap(interval)
        val timeFilter = FilterStatement(queryInfo.dataSetInfo.schema.timeField, None, Relation.inRange,
                                         Seq(overlaps.getStart, overlaps.getEnd).map(TimeField.TimeFormat.print))
        worker ? queryInfo.query.copy(filter = timeFilter +: queryInfo.query.filter)
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

  private def calculateNext(interval: TInterval, timeSpend: Long, boundary: TInterval): TInterval = {
    val newDuration = Math.max(config.MinTimeGap.toMillis, (interval.toDurationMillis * responseTime / timeSpend.toDouble).toLong)
    val startTime = Math.max(boundary.getStartMillis, interval.getStartMillis - newDuration)
    new TInterval(startTime, interval.getStartMillis)
  }
}

object ReactiveBerryClient {
  def props(jsonParser: JSONParser, dataManager: ActorRef, planner: QueryPlanner, config: Config, responseTime: Long)
           (implicit ec: ExecutionContext) = {
    Props(new ReactiveBerryClient(jsonParser, dataManager, planner, config, responseTime, defaultMaker))
  }

  def props(jsonParser: JSONParser, dataManager: ActorRef, planner: QueryPlanner, config: Config,
            childMaker: (ActorRefFactory, ReactiveBerryClient) => ActorRef, responseTime: Long)
           (implicit ec: ExecutionContext) = {
    Props(new ReactiveBerryClient(jsonParser, dataManager, planner, config, responseTime, childMaker))
  }

  def defaultMaker(context: ActorRefFactory, client: ReactiveBerryClient)(implicit ec: ExecutionContext): ActorRef = {
    context.actorOf(RESTFulBerryClient.props(client.jsonParser, client.dataManager, client.planner, suggestView = false, client.config))
  }

  trait IPostTransform {
    def transform(jsValue: JsValue): JsValue
  }

  case object NoTransform extends IPostTransform {
    override def transform(jsValue: JsValue): JsValue = jsValue
  }

  case class Request(requests: Seq[(JsValue, IPostTransform)])

}
