package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, Props, Stash}
import akka.pattern.ask
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.TInterval
import edu.uci.ics.cloudberry.zion.actor.DataStoreManager.AskInfo
import edu.uci.ics.cloudberry.zion.actor.RESTFulBerryClient.NoSuchDataset
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.impl.QueryPlanner.IMerger
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, JSONParser, QueryPlanner}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.DateTime
import play.api.libs.json.{JsArray, JsValue, Json}

import scala.concurrent.ExecutionContext

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

  //WARNING: all these evil mutable value SHALL NOT be read in the future thread
  var curJson: JsValue = _
  var curQuery: Query = _
  var curInfo: DataSetInfo = _
  var curSender: ActorRef = _
  var curPostProp: (JsArray) => JsValue = _
  var queryBoundary: TInterval = _
  var accumulateResult: JsArray = _
  var merger: IMerger = _
  var lastAskTS: DateTime = _

  override def receive: Receive = {
    case request: Request =>
      val query = jsonParser.parse(request.json)
      val immutableSender = sender()
      curSender = immutableSender
      curJson = request.json
      curPostProp = request.postProcess
      dataManager ? AskInfo(query.dataset) map {
        case Some(info: DataSetInfo) => self ! Initial(request.json, query, info)
        case None => immutableSender ! NoSuchDataset(query.dataset)
      }
    case initial: Initial if initial.json == curJson =>
      log.info("initializing current json")
      init(initial.query, initial.info)
      import scala.concurrent.duration._
      val initialDuration = (30 days).toMillis
      val interval = calculateFirst(queryBoundary, initialDuration)
      issueAQuery(interval)
      context.become(askSlice(interval))
  }

  private def init(query: Query, info: DataSetInfo): Unit = {
    curQuery = query.copy(filter = query.filter.filterNot(_.fieldName == info.schema.timeField))
    curInfo = info
    queryBoundary = query.getTimeInterval(info.schema.timeField).getOrElse(new TInterval(info.dataInterval.getStart, DateTime.now))
    accumulateResult = JsArray()
    merger = planner.calculateMergeFunc(query, info.schema)
    lastAskTS = DateTime.now
  }

  private def askSlice(interval: TInterval): Receive = {
    case result: PartialResult if result.query == curQuery =>
      accumulateResult = merger(Seq(accumulateResult, result.json))
      curSender ! curPostProp(accumulateResult)

      val timeSpend = DateTime.now.getMillis - lastAskTS.getMillis
      val nextInterval = calculateNext(interval, timeSpend)
      if (nextInterval.toDurationMillis == 0) {
        context.become(receive)
      } else {
        issueAQuery(nextInterval)
        context.become(askSlice(nextInterval))
      }
    case newRequest: Request =>
      stash()
      unstashAll()
      context.become(receive)
  }

  private def issueAQuery(interval: TInterval): Unit = {
    lastAskTS = DateTime.now
    val intervalFilter = FilterStatement(curInfo.schema.timeField, None, Relation.inRange,
                                         Seq(interval.getStart, interval.getEnd).map(TimeField.TimeFormat.print))
    val immutableQuery = curQuery.copy()
    (worker ? curQuery.copy(filter = intervalFilter +: curQuery.filter)).map {
      case result: JsArray =>
        self ! PartialResult(immutableQuery, result)
    }
  }

  private def calculateFirst(entireInterval: TInterval, gap: Long): TInterval = {
    val startTime = Math.max(entireInterval.getEndMillis - gap, entireInterval.getStartMillis)
    new TInterval(startTime, entireInterval.getEndMillis)
  }

  private def calculateNext(interval: TInterval, timeSpend: Long): TInterval = {
    val newDuration = (interval.toDurationMillis * responseTime / timeSpend.toDouble).toLong
    val startTime = Math.max(queryBoundary.getStartMillis, interval.getStartMillis - newDuration)
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
    context.actorOf(RESTFulBerryClient.props(client.jsonParser, client.dataManager, client.planner, client.config))
  }

  case class Request(json: JsValue, postProcess: (JsArray) => JsValue)

  case class Initial(json: JsValue, query: Query, info: DataSetInfo)

  case class PartialResult(query: Query, json: JsArray)

}
