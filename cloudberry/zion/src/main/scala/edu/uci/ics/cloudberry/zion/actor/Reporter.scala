package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, PoisonPill}
import edu.uci.ics.cloudberry.zion.TInterval
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, JsValue, Json, Writes}

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._


class Reporter(out: ActorRef)(implicit val ec: ExecutionContext) extends Actor with ActorLogging {

  import Reporter._

  private val queue: mutable.Queue[PartialResult] = new mutable.Queue[PartialResult]()

  private var limit: FiniteDuration = _
  private var timer: Cancellable = _

  override def receive: Actor.Receive = commonReceive orElse {
    case result: PartialResult =>
       queue.enqueue(result)
    case TimeToReport => {
      if (queue.isEmpty) {
        timer.cancel()
        context.become(hungry(DateTime.now()), discardOld = false)
      } else {
        val result = queue.dequeue()
        out ! Json.toJson(result.content)
      }
    }
    case any =>
      log.error(s"unknown msg: $any")
  }

  private def hungry(since: DateTime): Actor.Receive = commonReceive orElse {
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

  private def commonReceive: Actor.Receive = {
    case Reset(l) =>
      limit = l
      timer = context.system.scheduler.schedule(0 seconds, limit, self, TimeToReport)
      context.become(receive)
    case fin : Fin => {
      if (queue.nonEmpty) {
        /*
          Logistic Here is when we have delta result, we need to
          merge these results in the queue. If we have accumulated result,
          we just need to return the newest accumulated result.
        */
        if(fin.returnDelta){
          var resultList = List[JsValue]()
          val lastContent = queue.dequeueAll(resultSect=>
          {
            val resultContent = resultSect.content
            resultContent.\\("value").head.as[Array[JsValue]].foreach(
              v=> v.as[List[JsValue]].foreach( vc=> resultList = resultList :+ vc )
            )
            true
          }
          ).last.content
          var wrap = Json.toJson(List[List[JsValue]](resultList))
          var to_return = lastContent.as[JsObject]
          to_return = to_return.-("value")

          to_return = to_return.+("value",wrap)

          out ! Json.toJson(to_return)
        }
        else{
          out ! Json.toJson(queue.dequeueAll(_ => true).last.content)
        }
        //TODO remove this special DONE message
        out ! fin.lastMsg // notifying the client the processing is done
      }
      timer.cancel()
      context.become(receive)
    }
  }

}

object Reporter {

  case object TimeToReport

  case class Reset(limit: FiniteDuration)

  case class PartialResult(fromTS: Long, toTS: Long, progress: Double, content: JsValue)

  case class Fin(lastMsg: JsValue, returnDelta: Boolean)

  implicit val partialResultWriter: Writes[PartialResult] = Json.writes[PartialResult]

}

