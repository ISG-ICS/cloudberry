package edu.uci.ics.cloudberry.zion.model.actor

import akka.actor.{Actor, ActorLogging}
import edu.uci.ics.cloudberry.zion.model.schema.{Append, Query}

class DataSetActor() extends Actor with ActorLogging {
  override def receive: Receive = {
    case Query =>
    case Append =>
    case _ =>
  }
}
