package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, Props}
import akka.pattern.ask
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator, IQLGeneratorFactory}
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, Stats}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

class DataStoreManager(initialMetaData: Map[String, DataSetInfo],
                       val conn: IDataConn,
                       val queryParserFactory: IQLGeneratorFactory,
                       val config: Config,
                       val childMaker: (ActorRefFactory, String, Seq[Any]) => ActorRef)
                      (implicit ec: ExecutionContext) extends Actor with ActorLogging {

  import DataStoreManager._

  type TMetaMap = scala.collection.mutable.Map[String, DataSetInfo]
  type TViewMap = scala.collection.mutable.Map[String, String]
  type TSet = scala.collection.mutable.Set[String]
  type TJodaInterval = org.joda.time.Interval

  val metaData: TMetaMap = scala.collection.mutable.Map[String, DataSetInfo](initialMetaData.toList: _*)
  //TODO a bad pattern to create the view, need to embed to the DataSetAgent to make the state machine
  val creatingSet: TSet = scala.collection.mutable.Set[String]()
  val managerParser = queryParserFactory()
  implicit val askTimeOut: Timeout = Timeout(config.DataManagerAppendViewTimeOut)

  override def receive: Receive = {
    case register: Register => ???
    case deregister: Deregister => ???
    case query: Query => answerQuery(query)
    case append: AppendView => answerQuery(append)
    case create: CreateView => createView(create)
    case drop: DropView => ???
    case askInfo: AskInfoMsg =>
      sender ! {
        metaData.get(askInfo.who) match {
          case Some(info) => info +: metaData.filter(_._2.createQueryOpt.exists(q => q.dataset == askInfo.who)).values.toList
          case None => Seq.empty
        }
      }

    case info: DataSetInfo =>
      metaData += info.name -> info
      creatingSet.remove(info.name)
  }

  private def answerQuery(query: IQuery): Unit = {
    if (metaData.get(query.dataset).isEmpty) return

    val actor = context.child(query.dataset).getOrElse {
      val schema: Schema = metaData(query.dataset).schema
      childMaker(context, query.dataset, Seq(schema, queryParserFactory(), conn, ec))
    }
    query match {
      case q: Query => actor.forward(q)
      case q: AppendView => (actor ? q) map {
        case true => updateStats(q.dataset)
        case false =>
      }
      case _ =>
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
    val queryString = managerParser.generate(create, sourceInfo.schema)
    conn.postControl(queryString).map {
      case true =>
        val now = DateTime.now()
        collectStats(create.dataset, schema).map { case (interval, size) =>
          self ! DataSetInfo(create.dataset, Some(create.query), schema, interval, Stats(now, now, now, size))
        }
      case false => ???
    }
  }

  private def updateStats(dataset: String): Unit = {
    val originalInfo = metaData(dataset)
    collectStats(dataset, originalInfo.schema).map { case (interval, size) =>
      self ! originalInfo.copy(dataInterval = interval, stats = originalInfo.stats.copy(cardinality = size))
    }
  }

  private def collectStats(dataset: String, schema: Schema): Future[(TJodaInterval, Long)] = {
    val minTimeQuery = Query(dataset, globalAggr = Some(GlobalAggregateStatement(AggregateStatement(schema.timeField, Min, "min"))))
    val maxTimeQuery = Query(dataset, globalAggr = Some(GlobalAggregateStatement(AggregateStatement(schema.timeField, Max, "max"))))
    val cardinalityQuery = Query(dataset, globalAggr = Some(GlobalAggregateStatement(AggregateStatement("*", Count, "count"))))
    val parser = queryParserFactory()
    import TimeField.TimeFormat
    for {
      minTime <- conn.postQuery(parser.generate(minTimeQuery, schema)).map(r => (r \ "min").as[String])
      maxTime <- conn.postQuery(parser.generate(maxTimeQuery, schema)).map(r => (r \ "max").as[String])
      cardinality <- conn.postQuery(parser.generate(cardinalityQuery, schema)).map(r => (r \ "count").as[Long])
    } yield (new TJodaInterval(TimeFormat.parseDateTime(minTime), TimeFormat.parseDateTime(maxTime)), cardinality)
  }

}

object DataStoreManager {

  def props(initialMetaData: Map[String, DataSetInfo],
            conn: IDataConn,
            queryParserFactory: IQLGeneratorFactory,
            config: Config)
           (implicit ec: ExecutionContext) = {
    Props(new DataStoreManager(initialMetaData, conn, queryParserFactory, config, defaultMaker))
  }

  def defaultMaker(context: ActorRefFactory, name: String, args: Seq[Any])(implicit ec: ExecutionContext): ActorRef = {
    context.actorOf(DataSetAgent.props(
      args(0).asInstanceOf[Schema], args(1).asInstanceOf[IQLGenerator], args(2).asInstanceOf[IDataConn]), name)
  }


  case class AskInfoMsg(who: String)

  case class Register(dataset: String, schema: Schema)

  case class Deregister(dataset: String)

}
