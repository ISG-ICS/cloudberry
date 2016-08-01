package edu.uci.ics.cloudberry.zion.model.actor

import akka.actor.Actor
import edu.uci.ics.cloudberry.zion.model.impl.DataSetInfo
import edu.uci.ics.cloudberry.zion.model.schema.{AppendView, CreateView, DropView, Query}

class DataManager extends Actor {

  import DataManager._

  override def receive: Receive = {
    case register: Register => ???
    case deregister: Deregister => ???
    case query: Query => ???
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
