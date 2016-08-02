package edu.uci.ics.cloudberry.zion.model.actor

import akka.actor.{Actor, Props}
import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import edu.uci.ics.cloudberry.zion.model.impl.{AQLQueryParser, DataSetInfo}
import edu.uci.ics.cloudberry.zion.model.schema._

import scala.concurrent.ExecutionContext

class DataManager(val conn: IDataConn)
                 (implicit ec: ExecutionContext) extends Actor {

  import DataManager._

  type MutableMap[String, DataSetInfo] = scala.collection.mutable.Map[String, DataSetInfo]

  val aqlQueryParser = new AQLQueryParser
  val metaData: scala.collection.mutable.Map[String, DataSetInfo] = ???
  val viewRelation: scala.collection.mutable.Map[String, Seq[String]] = ???

  override def receive: Receive = {
    case register: Register => ???
    case deregister: Deregister => ???
    case query: Query => answerQuery(query)
    case append: AppendView => ???
    case create: CreateView => this.createView(create)
    case drop: DropView => ???
    case askInfo: AskInfoMsg => metaData.get(askInfo.who) match {
      case Some(info) => info +: viewRelation(info.name).map(metaData(_))
      case None => sender() ! Seq.empty
    }
  }

  private def answerQuery(query: Query): Unit = {
    context.child(query.dataset).getOrElse {
      val schema: Schema = metaData(query.dataset).schema
      context.actorOf(Props(classOf[DataSetActor], schema, new AQLQueryParser, conn, ec), query.dataset)
    } forward query
  }

  private def createView(create: CreateView): Unit = {
    if (metaData.contains(create.dataset)) return
    val schema = aqlQueryParser.calcResultSchema(create.query, metaData(create.dataset).schema)
    ???
  }
}

object DataManager {

  case class AskInfoMsg(who: String)

  case class Register(dataSetInfo: DataSetInfo)

  case class Deregister(dataSetInfo: DataSetInfo)

}
