package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash, Status}
import akka.pattern.ask
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.TInterval
import edu.uci.ics.cloudberry.zion.actor.DataStoreManager.{AskInfo, AskInfoAndViews}
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.impl.QueryPlanner.IMerger
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, JSONParser, QueryPlanner}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.DateTime
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * A reactive client which will continuously feed the result back to user
  * One user should only attach to one ReactiveClient
  *
  * TODO: a better design should be a reception actor that directs the slicing query and the entire query to different
  * workers(actors).
  * TODO: merge the multiple times AskViewsInfos
  */
class BerryClient(val jsonParser: JSONParser,
                  val dataManager: ActorRef,
                  val planner: QueryPlanner,
                  val config: Config,
                  val out: Option[ActorRef]
                 )(implicit ec: ExecutionContext) extends Actor with Stash with ActorLogging {

  import BerryClient._

  implicit val askTimeOut: Timeout = config.UserTimeOut
  private val minTimeGap = config.MinTimeGap

  private case class QueryInfo(query: Query, dataSetInfo: DataSetInfo, queryBound: TInterval, merger: IMerger)

  private case class QueryGroup(ts: DateTime, curSender: ActorRef, queries: Seq[QueryInfo], postTransform: IPostTransform)

  private case class Initial(ts: DateTime, sender: ActorRef, targetMillis: Long, queries: Seq[Query], infos: Seq[DataSetInfo], postTransform: IPostTransform)

  private case class PartialResult(queryGroup: QueryGroup, jsons: Seq[JsArray])

  //WARNING: Evil mutable value SHALL NOT be read/write in the future thread
  var curKey: DateTime = _

  override def receive: Receive = {
    case json: JsValue =>
      handleNewRequest(Request(json, NoTransform), out.getOrElse(sender()))
    case request: Request =>
      handleNewRequest(request, out.getOrElse(sender()))
    case initial: Initial if initial.ts == curKey =>
      val queryInfos = initial.queries.zip(initial.infos).map {
        case (query, info) =>
          val bound = query.getTimeInterval(info.schema.timeField).getOrElse(new TInterval(info.dataInterval.getStart, DateTime.now))
          val merger = planner.calculateMergeFunc(query, info.schema)
          val queryWOTime = query.copy(filter = query.filter.filterNot(_.fieldName == info.schema.timeField))
          QueryInfo(queryWOTime, info, bound, merger)
      }
      val min = queryInfos.map(_.queryBound.getStartMillis).min
      val max = queryInfos.map(_.queryBound.getEndMillis).max
      val boundary = new TInterval(min, max)

      val initialDuration = config.FirstQueryTimeGap.toMillis
      val interval = calculateFirst(boundary, initialDuration)
      val queryGroup = QueryGroup(initial.ts, initial.sender, queryInfos, initial.postTransform)
      val initResult = Seq.fill(queryInfos.size)(JsArray())
      issueQueryGroup(interval, queryGroup)
      context.become(askSlice(initial.targetMillis, interval, boundary, queryGroup, initResult, askTime = DateTime.now), discardOld = true)
  }

  private def handleNewRequest(request: Request, curSender: ActorRef): Unit = {
    val (queries, runOption) = jsonParser.parse(request.json)
    val key = DateTime.now()
    curKey = key
    val fDataInfos = Future.traverse(queries) { query =>
      dataManager ? AskInfo(query.dataset)
    }.map(seq => seq.map(_.asInstanceOf[Option[DataSetInfo]]))

    fDataInfos.foreach { seqInfos =>
      if (seqInfos.exists(_.isEmpty)) {
        curSender ! noSuchDatasetJson(queries(seqInfos.indexOf(None)).dataset)
      } else {
        if (runOption.sliceMills <= 0) {
          val result = Future.traverse(queries)(q => solveAQuery(q)).map(JsArray.apply)
          result.foreach(curSender ! request.postTransform.transform(_))
        } else {
          val targetMillis = runOption.sliceMills
          self ! Initial(key, curSender, targetMillis, queries, seqInfos.map(_.get), request.postTransform)
        }
      }
    }
  }

  private def askSlice(targetInvertal: Long,
                       curInterval: TInterval,
                       boundary: TInterval,
                       queryGroup: QueryGroup,
                       accumulateResults: Seq[JsArray],
                       askTime: DateTime): Receive = {
    case result: PartialResult if result.queryGroup.ts == curKey =>
      val mergedResults = queryGroup.queries.zipWithIndex.map { case (q, idx) =>
        q.merger(Seq(accumulateResults(idx), result.jsons(idx)))
      }
      queryGroup.curSender ! queryGroup.postTransform.transform(JsArray(mergedResults))

      val timeSpend = DateTime.now.getMillis - askTime.getMillis
      val nextInterval = calculateNext(targetInvertal, curInterval, timeSpend, boundary)
      if (nextInterval.toDurationMillis == 0) {
        queryGroup.curSender ! BerryClient.Done // notifying the client the processing is done
        suggestViews(queryGroup)
        context.become(receive, discardOld = true)
      } else {
        issueQueryGroup(nextInterval, queryGroup)
        context.become(askSlice(targetInvertal, nextInterval, boundary, queryGroup, mergedResults, DateTime.now), discardOld = true)
      }
    case json: JsValue =>
      stash()
      unstashAll()
      context.become(receive, discardOld = true)
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
  private def issueQueryGroup(interval: TInterval, queryGroup: QueryGroup): Unit = {
    val futures = Future.traverse(queryGroup.queries) { queryInfo =>
      if (queryInfo.queryBound.overlaps(interval)) {
        val overlaps = queryInfo.queryBound.overlap(interval)
        val timeFilter = FilterStatement(queryInfo.dataSetInfo.schema.timeField, None, Relation.inRange,
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

  protected def solveAQuery(query: Query): Future[JsValue] = {
    val fInfos = dataManager ? AskInfoAndViews(query.dataset) map {
      case seq: Seq[_] if seq.forall(_.isInstanceOf[DataSetInfo]) =>
        seq.map(_.asInstanceOf[DataSetInfo])
      case _ => Seq.empty
    }

    fInfos.flatMap {
      case seq if seq.isEmpty =>
        Future(noSuchDatasetJson(query.dataset))
      case infos: Seq[DataSetInfo] =>
        val (queries, merger) = planner.makePlan(query, infos.head, infos.tail)
        val fResponse = Future.traverse(queries) { subQuery =>
          dataManager ? subQuery
        }.map(seq => seq.map(_.asInstanceOf[JsValue]))

        fResponse.map { responses => merger(responses) }
    }
  }

}

object BerryClient {

  val Done = Json.obj("key" -> JsString("done"))

  object Interrupt

  def props(jsonParser: JSONParser, dataManager: ActorRef, planner: QueryPlanner, config: Config)
           (implicit ec: ExecutionContext) = {
    Props(new BerryClient(jsonParser, dataManager, planner, config, None))
  }

  def props(jsonParser: JSONParser, dataManager: ActorRef, planner: QueryPlanner, config: Config, out: ActorRef)
           (implicit ec: ExecutionContext) = {
    Props(new BerryClient(jsonParser, dataManager, planner, config, Some(out)))
  }

  trait IPostTransform {
    def transform(jsValue: JsValue): JsValue
  }

  case object NoTransform extends IPostTransform {
    override def transform(jsValue: JsValue): JsValue = jsValue
  }

  //TODO merge the post transform into the json model
  case class Request(json: JsValue, postTransform: IPostTransform)

  def noSuchDatasetJson(name: String): JsValue = {
    JsObject(Seq("error" -> JsString(s"Dataset $name does not exist")))
  }
}
