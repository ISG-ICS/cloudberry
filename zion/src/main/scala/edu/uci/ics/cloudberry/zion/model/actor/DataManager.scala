package edu.uci.ics.cloudberry.zion.model.actor

import akka.actor.Actor
import edu.uci.ics.cloudberry.zion.model.schema.{AppendSelfQuery, DropQuery, Query}

class DataManager extends Actor {

  import DataManager._

  override def receive: Receive = {
    case registerDataSet =>
    case deregisterDataSet =>
    case query: Query =>
    case append: AppendSelfQuery =>
    case createView => //should be unified as append
    case drop: DropQuery =>
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

}
