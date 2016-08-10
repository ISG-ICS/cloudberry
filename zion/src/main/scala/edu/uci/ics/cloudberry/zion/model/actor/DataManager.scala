package edu.uci.ics.cloudberry.zion.model.actor

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.ask
import akka.util.Timeout
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQueryParserFactory}
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, Stats}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext

class DataManager(initialMetaData: Map[String, DataSetInfo],
                  val conn: IDataConn,
                  val queryParserFactory: IQueryParserFactory,
                  val config: Config)
                 (implicit ec: ExecutionContext) extends Actor with ActorLogging {

  import DataManager._

  type TMetaMap = scala.collection.mutable.Map[String, DataSetInfo]
  type TViewMap = scala.collection.mutable.Map[String, String]
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
    case askInfo: AskInfoMsg => metaData.get(askInfo.who) match {
      case Some(info) => info +: metaData.filter(_._2.createQueryOpt.exists(q => q.dataset == askInfo.who)).values.toList
      case None => sender() ! Seq.empty
    }

    case dataset: DataSetInfo =>
      metaData += dataset.name -> dataset
  }

  private def answerQuery(query: IQuery): Unit = {
    val actor = context.child(query.dataset).getOrElse {
      val schema: Schema = metaData(query.dataset).schema
      context.actorOf(Props(classOf[DataSetAgent], schema, queryParserFactory(), conn, ec), query.dataset)
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
    if (metaData.contains(create.dataset)) return
    val sourceInfo = metaData(create.dataset)
    val schema = managerParser.calcResultSchema(create.query, sourceInfo.schema)
    val queryString = managerParser.parse(create.query, sourceInfo.schema)
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

  private def updateStats(dataset: String): Unit = {
    ???
  }

}

object DataManager {

  def props(initialMetaData: Map[String, DataSetInfo],
            conn: IDataConn,
            queryParserFactory: IQueryParserFactory,
            config: Config)
           (implicit ec: ExecutionContext) = {
    Props(new DataManager(initialMetaData, conn, queryParserFactory, config))
  }

  case class AskInfoMsg(who: String)

  case class Register(dataset: String, schema: Schema)

  case class Deregister(dataset: String)

}
