package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging}
import akka.pattern.ask
import edu.uci.ics.cloudberry.zion.actor.CounterAgent.CountAndRate
import edu.uci.ics.cloudberry.zion.model.schema.Query

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

class CounterAgent(val initialMap: Map[String, CountAndRate],
                   val firstUpdateDuration: FiniteDuration,
                   val updateCycle: FiniteDuration)
                  (implicit ec: ExecutionContext)
  extends Actor with ActorLogging {

  import CounterAgent._

  val countMap = scala.collection.mutable.Map[String, CountAndRate]()

  def receive = {
    case ask: AskCount => ???
      sender() ! countMap.getOrElse(ask.dataset, CountAndRate(0, 0))
    case AskDataset =>
      (context.parent ? Query(???)).map {
        ???
      }
    case update: UpdateCount =>
      ???
  }

  context.system.scheduler.schedule(firstUpdateDuration, updateCycle, self, AskDataset)

}

object CounterAgent {

  case class CountAndRate(count: Int, ratePerSecond: Int)

  case object AskDataset

  case object InitialCount

  case class UpdateCount(dataset: String, count: Int)

  case class AskCount(dataset: String)

}
