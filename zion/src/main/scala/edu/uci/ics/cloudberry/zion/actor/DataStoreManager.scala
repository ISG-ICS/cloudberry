package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, Props, Stash}
import akka.pattern.ask
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator, IQLGeneratorFactory}
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, Stats}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.DateTime
import play.api.libs.json.{JsArray, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class DataStoreManager(metaDataset: String,
                       val conn: IDataConn,
                       val queryGenFactory: IQLGeneratorFactory,
                       val config: Config,
                       val childMaker: (ActorRefFactory, String, Seq[Any]) => ActorRef)
                      (implicit ec: ExecutionContext) extends Actor with Stash with ActorLogging {

  import DataStoreManager._

  type TMetaMap = scala.collection.mutable.Map[String, DataSetInfo]
  type TViewMap = scala.collection.mutable.Map[String, String]
  type TSet = scala.collection.mutable.Set[String]
  type TJodaInterval = org.joda.time.Interval

  val metaData: TMetaMap = scala.collection.mutable.Map[String, DataSetInfo]()
  //TODO a bad pattern to create the view, need to embed to the DataSetAgent to make the state machine
  val creatingSet: TSet = scala.collection.mutable.Set[String]()
  val managerParser = queryGenFactory()
  implicit val askTimeOut: Timeout = Timeout(config.DataManagerAppendViewTimeOut)

  val metaActor = childMaker(context, "meta", Seq(DataSetInfo.MetaSchema, queryGenFactory(), conn, ec))

  override def preStart(): Unit = {
    metaActor ? Query(metaDataset, select = Some(SelectStatement(Seq.empty, 100000000, 0, Seq.empty))) map {
      case jsArray: JsArray =>
        val records = jsArray.as[Seq[DataSetInfo]]
        metaData ++= records.map(info => info.name -> info)
        self ! Prepared
      case any => log.error(s"received unknown object from meta actor: $any ")
    }
  }

  override def postStop(): Unit = {
    metaData.clear()
  }

  override def receive: Receive = preparing

  def preparing: Receive = {
    case Prepared =>
      unstashAll()
      context.become(normal, discardOld = true)
    case _ => stash()
  }

  def normal: Receive = {
    case AreYouReady => sender() ! true
    case register: Register => ???
    case deregister: Deregister => ???
    case query: Query => answerQuery(query)
    case append: AppendView => answerQuery(append, Some(DateTime.now()))
    case append: AppendViewAutomatic =>
      metaData.get(append.dataset) match {
        case Some(info) =>
          info.createQueryOpt match {
            case Some(createQuery) =>
              if (createQuery.filter.exists(_.fieldName == info.schema.timeField)) {
                log.error("the create view should not contains the time dimension")
              } else {
                val now = DateTime.now()
                val compensate = FilterStatement(info.schema.timeField, None, Relation.inRange,
                                                 Seq(info.stats.lastModifyTime, now).map(TimeField.TimeFormat.print))
                val appendQ = createQuery.copy(filter = compensate +: createQuery.filter)
                answerQuery(AppendView(info.name, appendQ), Some(now))
              }
            case None => log.warning(s"can not append to a base dataset: $append.dataset.")
          }
        case None => log.warning(s"view $append.dataset does not exist.")
      }
    case create: CreateView => createView(create)
    case drop: DropView => ???
    case askInfo: AskInfoAndViews =>
      sender ! {
        metaData.get(askInfo.who) match {
          case Some(info) => info +: metaData.filter(_._2.createQueryOpt.exists(q => q.dataset == askInfo.who)).values.toList
          case None => Seq.empty
        }
      }
    case askInfo: AskInfo =>
      sender ! metaData.get(askInfo.who)

    case info: DataSetInfo =>
      metaData += info.name -> info
      creatingSet.remove(info.name)
      flushMetaData()
    case FlushMeta => flushMetaData()
  }

  //persistent metadata periodically
  context.system.scheduler.schedule(config.ViewMetaFlushInterval, config.ViewMetaFlushInterval, self, FlushMeta)

  private def answerQuery(query: IQuery, now: Option[DateTime] = None): Unit = {
    if (!metaData.contains(query.dataset)) return

    val actor = context.child("data-" + query.dataset).getOrElse {
      val info = metaData(query.dataset)
      val schema: Schema = info.schema
      val ret = childMaker(context, "data-" + query.dataset, Seq(schema, queryGenFactory(), conn, ec))
      // make views append self periodically
      info.createQueryOpt match {
        case Some(_) =>
          context.system.scheduler.schedule(config.ViewUpdateInterval, config.ViewUpdateInterval, self, AppendViewAutomatic(query.dataset))
        case None =>
      }
      ret
    }
    query match {
      case q: Query => actor.forward(q)
      case q: AppendView =>
        (actor ? q) map {
          case true => updateStats(q.dataset, now.get)
          case false =>
        }
      case _ => ???
    }
  }

  private def createView(create: CreateView): Unit = {
    if (metaData.contains(create.dataset) || !metaData.contains(create.query.dataset) || creatingSet.contains(create.dataset)) {
      log.warning(s"invalid dataset in the CreateView msg: $create")
      return
    }
    creatingSet.add(create.dataset)
    val sourceInfo = metaData(create.query.dataset)
    val schema = managerParser.calcResultSchema(create.query, sourceInfo.schema)
    val now = DateTime.now()
    val fixEndFilter = FilterStatement(sourceInfo.schema.timeField, None, Relation.<, Seq(TimeField.TimeFormat.print(now)))
    val newCreateQuery = create.query.copy(filter = fixEndFilter +: create.query.filter)
    val queryString = managerParser.generate(create.copy(query = newCreateQuery), sourceInfo.schema)
    conn.postControl(queryString) onSuccess {
      case true =>
        collectStats(create.dataset, schema) onComplete {
          case Success((interval, size)) =>
            self ! DataSetInfo(create.dataset, Some(create.query), schema, interval, Stats(now, now, now, size))
          case Failure(ex) =>
            log.error(s"collectStats error: $ex")
        }
      case false =>
        log.error("Failed to create view:" + create)
    }
  }

  private def updateStats(dataset: String, modifyTime: DateTime): Unit = {
    val originalInfo = metaData(dataset)
    collectStats(dataset, originalInfo.schema) onSuccess { case (interval, size) =>
      //TODO need to think the difference between the txn time and the ingest time
      self ! originalInfo.copy(dataInterval = interval, stats = originalInfo.stats.copy(lastModifyTime = modifyTime, cardinality = size))
    }
  }

  private def collectStats(dataset: String, schema: Schema): Future[(TJodaInterval, Long)] = {
    val minTimeQuery = Query(dataset, globalAggr = Some(GlobalAggregateStatement(AggregateStatement(schema.timeField, Min, "min"))))
    val maxTimeQuery = Query(dataset, globalAggr = Some(GlobalAggregateStatement(AggregateStatement(schema.timeField, Max, "max"))))
    val cardinalityQuery = Query(dataset, globalAggr = Some(GlobalAggregateStatement(AggregateStatement("*", Count, "count"))))
    val parser = queryGenFactory()
    import TimeField.TimeFormat
    for {
      minTime <- conn.postQuery(parser.generate(minTimeQuery, schema)).map(r => (r \\ "min").head.as[String])
      maxTime <- conn.postQuery(parser.generate(maxTimeQuery, schema)).map(r => (r \\ "max").head.as[String])
      cardinality <- conn.postQuery(parser.generate(cardinalityQuery, schema)).map(r => (r \\ "count").head.as[Long])
    } yield (new TJodaInterval(TimeFormat.parseDateTime(minTime), TimeFormat.parseDateTime(maxTime)), cardinality)
  }

  private def flushMetaData(): Unit = {
    metaActor ! UpsertRecord(metaDataset, Json.toJson(metaData.values.toSeq).asInstanceOf[JsArray])
  }
}

object DataStoreManager {

  def props(metaDataSet: String,
            conn: IDataConn,
            queryParserFactory: IQLGeneratorFactory,
            config: Config)
           (implicit ec: ExecutionContext) = {
    Props(new DataStoreManager(metaDataSet, conn, queryParserFactory, config, defaultMaker))
  }

  def defaultMaker(context: ActorRefFactory, name: String, args: Seq[Any])(implicit ec: ExecutionContext): ActorRef = {
    context.actorOf(DataSetAgent.props(
      args(0).asInstanceOf[Schema], args(1).asInstanceOf[IQLGenerator], args(2).asInstanceOf[IDataConn]), name)
  }


  case class AskInfoAndViews(who: String)

  case class AskInfo(who: String)

  case class Register(dataset: String, schema: Schema)

  case class Deregister(dataset: String)

  case object FlushMeta

  case object AreYouReady

  case object Prepared

  case class AppendViewAutomatic(dataset: String)

}
