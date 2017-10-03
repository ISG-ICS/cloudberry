package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill}
import edu.uci.ics.cloudberry.zion.TInterval
import org.joda.time.DateTime
import play.api.libs.json.{JsValue, Json, Writes}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


class Reporter(initialLimit: FiniteDuration, out: ActorRef)(implicit val ec: ExecutionContext) extends Actor with ActorLogging {

  import Reporter._

  private val queue: mutable.Queue[PartialResult] = new mutable.Queue[PartialResult]()

  private var limit: FiniteDuration = initialLimit
  private var timer = context.system.scheduler.schedule(limit, limit, self, TimeToReport)

  override def receive: Actor.Receive = {
    case UpdateInterval(l) =>
      limit = l
    case result: PartialResult => queue.enqueue(result)
    case TimeToReport => {
      if (queue.isEmpty) {
        timer.cancel()
        context.become(hungry(DateTime.now()), discardOld = false)
      } else {
        val result = queue.dequeue()
        out ! Json.toJson(result.content)
      }
    }
    case Fin => {
      if (queue.nonEmpty) {
        out ! Json.toJson(queue.dequeueAll(_ => true).last.content)
      }
      self ! PoisonPill // TODO to simplify the logic, one reporter is working for a specific query.
    }
    case any =>
      log.error(s"unknown msg: $any")
  }

  private def hungry(since: DateTime): Actor.Receive = {
    case UpdateInterval(l) =>
      limit = l
    case r: PartialResult =>
      out ! Json.toJson(r.content)
      val delay = new TInterval(since, DateTime.now())
      log.warning(s"delayed ${delay.toDurationMillis / 1000.0} seconds ")
      timer = context.system.scheduler.schedule(limit, limit, self, TimeToReport)
      context.unbecome()
    case TimeToReport =>
      log.warning(s"has nothing to report")
    case any =>
      log.warning(s"cannot recognize the message: $any")
  }

}

object Reporter {

  case object TimeToReport

  case class UpdateInterval(limit: FiniteDuration)

  case class PartialResult(fromTS: Long, toTS: Long, progress: Double, content: JsValue)

  case object Fin

  implicit val partialResultWriter: Writes[PartialResult] = Json.writes[PartialResult]

}

