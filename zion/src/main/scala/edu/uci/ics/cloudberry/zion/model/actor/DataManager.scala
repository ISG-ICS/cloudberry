package edu.uci.ics.cloudberry.zion.model.actor

import akka.actor.{Actor, ActorRef, Props}
import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import edu.uci.ics.cloudberry.zion.model.impl.{AQLQueryParser, DataSetInfo}
import edu.uci.ics.cloudberry.zion.model.schema._

import scala.concurrent.ExecutionContext

class DataManager(val conn: IDataConn)
                 (implicit ec: ExecutionContext) extends Actor {

  import DataManager._

  def answerQuery(query: Query, ref: ActorRef): Unit = {
    context.child(query.dataset).getOrElse {
      val schema: Schema = ???
      context.actorOf(Props(classOf[DataSetActor], schema, new AQLQueryParser, conn, ec), query.dataset)
    } forward query
  }

  override def receive: Receive = {
    case register: Register => ???
    case deregister: Deregister => ???
    case query: Query => answerQuery(query, sender())
    case append: AppendView => ???
    case createView: CreateView => ???
    case drop: DropView => ???
    case askInfo: AskInfoMsg => metaData.get(askInfo.who) match {
      case Some(info) => info +: viewRelation(info.name).map(metaData(_))
      case None => sender() ! Seq.empty
    }
  }

  val metaData: scala.collection.mutable.Map[String, DataSetInfo] = ???
  val viewRelation: scala.collection.mutable.Map[String, Seq[String]] = ???
}

object DataManager {

  case class AskInfoMsg(who: String)

  case class Register(dataSetInfo: DataSetInfo)

  case class Deregister(dataSetInfo: DataSetInfo)

}
