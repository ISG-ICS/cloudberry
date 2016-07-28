package edu.uci.ics.cloudberry.zion.model.actor

import akka.actor.Actor
import edu.uci.ics.cloudberry.zion.model.schema.Query

class QueryPlanner(dataManager: DataManager) extends Actor {
  override def receive: Receive = {
    case query: Query =>
//      validateDataset(query.dataset)
    case _ =>
  }
}
