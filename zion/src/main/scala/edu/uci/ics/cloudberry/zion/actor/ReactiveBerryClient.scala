package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorRef, Props}
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.impl.{JSONParser, QueryPlanner}

import scala.concurrent.ExecutionContext

/**
  * A reactive client which will continuously feed the result back to user
  */
class ReactiveBerryClient(val out: ActorRef, val jsonParser: JSONParser, val dataManager: ActorRef, val planner: QueryPlanner, val config: Config)
                         (implicit val ec: ExecutionContext) extends Actor {
  override def receive: Receive = ???
}

object ReactiveBerryClient {
  def props() = ??? //Props(
}
