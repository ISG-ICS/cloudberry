package edu.uci.ics.cloudberry.zion.model.actor

import akka.actor.{Actor, Props}
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQueryParserFactory}
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, Stats}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext

class DataManager(val conn: IDataConn, val queryParserFactory: IQueryParserFactory)
                 (implicit ec: ExecutionContext) extends Actor {

  import DataManager._

  val managerParser = queryParserFactory()
  val metaData: scala.collection.mutable.Map[String, DataSetInfo] = ???
  val viewRelation: scala.collection.mutable.Map[String, Seq[String]] = ???

  override def receive: Receive = {
    case register: Register => ???
    case deregister: Deregister => ???
    case query: Query => answerQuery(query)
    case append: AppendView => answerQuery(append)
    case create: CreateView => createView(create)
    case drop: DropView => ???
    case askInfo: AskInfoMsg => metaData.get(askInfo.who) match {
      case Some(info) => info +: viewRelation(info.name).map(metaData(_))
      case None => sender() ! Seq.empty
    }

    case dataset: DataSetInfo => metaData += dataset.name -> dataset
  }

  private def answerQuery(query: IQuery): Unit = {
    context.child(query.dataset).getOrElse {
      val schema: Schema = metaData(query.dataset).schema
      context.actorOf(Props(classOf[DataSetAgent], schema, queryParserFactory(), conn, ec), query.dataset)
    } forward query
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
}

object DataManager {

  case class AskInfoMsg(who: String)

  case class Register(dataSetInfo: DataSetInfo)

  case class Deregister(dataSetInfo: DataSetInfo)

}
