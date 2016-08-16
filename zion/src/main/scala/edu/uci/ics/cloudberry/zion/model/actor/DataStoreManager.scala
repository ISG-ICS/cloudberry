package edu.uci.ics.cloudberry.zion.model.actor

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
  type TJodaInterval = org.joda.time.Interval

  val managerParser = queryParserFactory()
  val metaData: TMetaMap = scala.collection.mutable.Map[String, DataSetInfo](initialMetaData.toList: _*)
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
    if (metaData.contains(create.dataset) || !metaData.contains(create.query.dataset)) {
      //TODO should respond an error msg to user
      log.error(s"invalid dataset in the CreateView msg: ${create}")
      return
    }
    val sourceInfo = metaData(create.query.dataset)
    val schema = managerParser.calcResultSchema(create.query, sourceInfo.schema)
    val queryString = managerParser.generate(create, sourceInfo.schema)
    conn.postControl(queryString).map {
      case true =>
        //TODO replace the following query with an actual query
        val now = DateTime.now()
        val interval = new org.joda.time.Interval(sourceInfo.dataInterval.getStart, now)
        val cardinality = sourceInfo.stats.cardinality

        self ! DataSetInfo(create.dataset, Some(create.query), schema, interval, Stats(now, now, now, cardinality))
      case false => ???
    }
  }

  private def collectStats(dataset: String): Future[(TJodaInterval, Int)] = ???

  private def updateStats(dataset: String): Unit = {
    //TODO replace it to an actual query
    val originalInfo = metaData(dataset)
    val now = DateTime.now()
    val interval = new org.joda.time.Interval(originalInfo.dataInterval.getStart, now)
    val cardinality = originalInfo.stats.cardinality + 1000

    self ! originalInfo.copy(dataInterval = interval, stats = originalInfo.stats.copy(cardinality = cardinality))
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
      args(0).asInstanceOf[Schema], args(1).asInstanceOf[IQLGenerator], args(2).asInstanceOf[IDataConn]))
  }


  case class AskInfoMsg(who: String)

  case class Register(dataset: String, schema: Schema)

  case class Deregister(dataset: String)

}
