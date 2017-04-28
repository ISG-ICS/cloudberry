package edu.uci.ics.cloudberry.zion.actor

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore._
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, Stats, UnresolvedSchema}
import edu.uci.ics.cloudberry.zion.model.impl.DataSetInfo._
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.{DateTime, Interval}
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.{Failure, Success}

class DataStoreManager(metaDataset: String,
                       val conn: IDataConn,
                       val queryGenFactory: IQLGeneratorFactory,
                       val config: Config,
                       val childMaker: DataStoreManager.ChildMakerFuncType)
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

  val metaActor: ActorRef = childMaker(AgentType.Meta, context, "meta", DataSetInfo.MetaDataDBName, DataSetInfo.MetaSchema, queryGenFactory(), conn, config)

  override def preStart(): Unit = {
    metaActor ? Query(metaDataset, select = Some(SelectStatement(Seq(DataSetInfo.MetaSchema.timeField), Seq(SortOrder.ASC), Int.MaxValue, 0, Seq.empty))) map {
      case jsArray: JsArray =>
        val schemaMap: mutable.Map[String, Schema] = new mutable.HashMap[String, Schema]
        jsArray.value.foreach { json =>
          val info = DataSetInfo.parse(json, schemaMap.toMap)
          metaData.put(info.name, info)
          schemaMap.put(info.name, info.schema)
        }
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
    case register: Register => registerNewDataset(sender(), register)
    case deregister: Deregister => deregisterDataSet(sender(), deregister)
    case query: Query => answerQuery(query)
    case append: AppendView => answerQuery(append, Some(DateTime.now()))
    case append: AppendViewAutomatic =>
      //TODO move updating logics to ViewDataAgent
      metaData.get(append.dataset) match {
        case Some(info) =>
          info.createQueryOpt match {
            case Some(createQuery) =>
              if (createQuery.filter.exists(_.field == info.schema.timeField)) {
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

  private def registerNewDataset(sender: ActorRef, registerTable: Register): Unit = {
    val dataSetName = registerTable.dataset
    val dataSetRawSchema = registerTable.schema

    if(!metaData.contains(dataSetName)){

      try{
        val primaryKey = dataSetRawSchema.primaryKey.map(dataSetRawSchema.getField(_).get)
        val timeField = dataSetRawSchema.getField(dataSetRawSchema.timeField) match {
          case Some(f) =>
            if(f.isInstanceOf[TimeField]){
              f.asInstanceOf[TimeField]
            } else {
              throw new QueryParsingException("Time field of " + dataSetRawSchema.typeName + "is not in TimeField format.\n")
            }
          case None =>
            throw new QueryParsingException("Time field is not specified for " + dataSetRawSchema.typeName + ".\n")
        }
        val resolvedSchema = Schema(dataSetRawSchema.typeName, dataSetRawSchema.dimension, dataSetRawSchema.measurement, primaryKey, timeField)

        //TODO: Send query to get actual information when a dataset is registered.
        val currentDateTime = new DateTime()
        val fakeStats = Stats(currentDateTime, currentDateTime, currentDateTime, 1000000)

        val fakeStartDate = new DateTime(2005, 3, 26, 12, 0, 0, 0)
        val fakeInterval = new Interval(fakeStartDate, currentDateTime)

        val registerDataSetInfo = DataSetInfo(dataSetName, None, resolvedSchema, fakeInterval, fakeStats)
        metaData.put(dataSetName, registerDataSetInfo)
        flushMetaData()

        sender ! DataManagerResponse(true, "Register Finished: dataset " + dataSetName + " has successfully registered.\n")

      } catch {
        case fieldNotFoundError: FieldNotFound => sender ! DataManagerResponse(false, "Register Denied. Field Not Found Error: " + fieldNotFoundError.fieldName + " is not found in dimensions and measurements: not a valid field.\n")
        case queryParsingError: QueryParsingException => sender ! DataManagerResponse(false, "Register Denied. Field Parsing Error: " + queryParsingError.getMessage)
        case NonFatal(e) => sender ! DataManagerResponse(false, "Register Denied. " + e.getMessage)
      }

    } else{
      sender ! DataManagerResponse(false, "Register Denied: dataset " + dataSetName + " already existed.\n")
    }
  }

  private def deregisterDataSet(sender: ActorRef, dropTable: Deregister): Unit = {
    val dropTableName = dropTable.dataset

    if(metaData.contains(dropTableName)){

      metaData.remove(dropTableName)
      context.child("data-" + dropTableName).foreach( child => child ! PoisonPill)
      val metaRecordFilter = FilterStatement(DataSetInfo.MetaSchema.fieldMap("name"), None, Relation.matches, Seq(dropTableName))
      metaActor ! DeleteRecord(metaDataset, Seq(metaRecordFilter))

      metaData.filter{ case(name, info) =>
        info.createQueryOpt.exists( query => query.dataset == dropTableName)
      }.foreach { case(name, info) =>
        metaActor ! DropView(name)
        context.child("data-" + name).foreach( child => child ! PoisonPill)
      }

      // Before retrieve subset of metaData using .filter or .filterNot, etc.,
      // Use .toMap method to change metaData into immutable map
      // Otherwise when metaData is clear, no information will be retained.
      val newMetaData = metaData.toMap.filterNot{ case(name, info) =>
        info.createQueryOpt.exists(q => q.dataset == dropTableName)
      }
      metaData.clear()
      metaData ++= newMetaData

      sender ! DataManagerResponse(true, "Deregister Finished: dataset " + dropTableName + " has successfully removed.\n")
    } else{
      sender ! DataManagerResponse(false, "Deregister Denied: dataset " + dropTableName + " does not exist in database.\n")
    }
  }

  private def answerQuery(query: IQuery, now: Option[DateTime] = None): Unit = {
    if (!metaData.contains(query.dataset)) return

    val actor = context.child("data-" + query.dataset).getOrElse {
      val info = metaData(query.dataset)
      val schema: Schema = info.schema
      info.createQueryOpt match {
        case Some(_) =>
          val ret = childMaker(AgentType.View, context, "data-" + query.dataset, query.dataset, schema, queryGenFactory(), conn, config)
          context.system.scheduler.schedule(config.ViewUpdateInterval, config.ViewUpdateInterval, self, AppendViewAutomatic(query.dataset))
          ret
        case None =>
          childMaker(AgentType.Origin, context, "data-" + query.dataset, query.dataset, schema, queryGenFactory(), conn, config)
      }
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
    val queryString = managerParser.generate(create.copy(query = newCreateQuery), Map(create.query.dataset -> sourceInfo.schema))
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
    val timeField = schema.timeField
    val minTimeQuery = Query(dataset, globalAggr = Some(GlobalAggregateStatement(AggregateStatement(timeField, Min, Field.as(Min(timeField), "min")))))
    val maxTimeQuery = Query(dataset, globalAggr = Some(GlobalAggregateStatement(AggregateStatement(timeField, Max, Field.as(Max(timeField), "max")))))
    val cardinalityQuery = Query(dataset, globalAggr = Some(GlobalAggregateStatement(AggregateStatement(schema.fieldMap("*"), Count, Field.as(Min(timeField), "count")))))
    val parser = queryGenFactory()
    import TimeField.TimeFormat
    for {
      minTime <- conn.postQuery(parser.generate(minTimeQuery, Map(dataset -> schema))).map(r => (r \\ "min").head.as[String])
      maxTime <- conn.postQuery(parser.generate(maxTimeQuery, Map(dataset -> schema))).map(r => (r \\ "max").head.as[String])
      cardinality <- conn.postQuery(parser.generate(cardinalityQuery, Map(dataset -> schema))).map(r => (r \\ "count").head.as[Long])
    } yield (new TJodaInterval(TimeFormat.parseDateTime(minTime), TimeFormat.parseDateTime(maxTime)), cardinality)
  }

  private def flushMetaData(): Unit = {
    metaActor ! UpsertRecord(metaDataset, Json.toJson(metaData.values.map(DataSetInfo.write(_))).asInstanceOf[JsArray])
  }

}

object DataStoreManager {

  object AgentType extends Enumeration {
    val Meta = Value("meta")
    val Origin = Value("origin")
    val View = Value("view")
  }

  type ChildMakerFuncType = (AgentType.Value, ActorRefFactory, String, String, Schema, IQLGenerator, IDataConn, Config) => ActorRef

  def props(metaDataSet: String,
            conn: IDataConn,
            queryParserFactory: IQLGeneratorFactory,
            config: Config)
           (implicit ec: ExecutionContext) = {
    Props(new DataStoreManager(metaDataSet, conn, queryParserFactory, config, defaultMaker))
  }

  def defaultMaker(agentType: AgentType.Value,
                   context: ActorRefFactory,
                   actorName: String,
                   dbName: String,
                   dbSchema: Schema,
                   qLGenerator: IQLGenerator,
                   conn: IDataConn,
                   appConfig: Config
                  )(implicit ec: ExecutionContext): ActorRef = {
    import AgentType._
    agentType match {
      case Meta =>
        context.actorOf(MetaDataAgent.props(dbName, dbSchema, qLGenerator, conn, appConfig), actorName)
      case Origin =>
        context.actorOf(OriginalDataAgent.props(dbName, dbSchema, qLGenerator, conn, appConfig), actorName)
      case View =>
        context.actorOf(ViewDataAgent.props(dbName, dbSchema, qLGenerator, conn, appConfig), actorName)
    }
  }


  case class AskInfoAndViews(who: String)

  case class AskInfo(who: String)

  case class Register(dataset: String, schema: UnresolvedSchema)

  object Register{
    implicit val registerReader: Reads[Register] = {
      (__ \ "dataset").read[String] and
      (__ \ "schema").read[UnresolvedSchema]
    }.apply(Register.apply _)
  }

  case class Deregister(dataset: String)

  object Deregister{
    implicit val deregisterReader: Reads[Deregister] =
      (__ \ "dataset").read[String].map { dataset => Deregister(dataset) }
  }

  case class DataManagerResponse(isSuccess: Boolean, message: String)

  case object FlushMeta

  case object AreYouReady

  case object Prepared

  case class AppendViewAutomatic(dataset: String)

}
