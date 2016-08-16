package edu.uci.ics.cloudberry.zion.model.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.schema.{AppendView, Query, Schema}
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext

class DataSetAgent(val schema: Schema, val queryParser: IQLGenerator, val conn: IDataConn)
                  (implicit ec: ExecutionContext)
  extends Actor with Stash with ActorLogging {

  var isUpdating = false

  override def receive: Receive = {
    case query: Query =>
      val curSender = sender()
      conn.postQuery(queryParser.generate(query, schema)).map(curSender ! _)
    case append: AppendView =>
      println(s"receive from $sender() at ${DateTime.now}")
      if (isUpdating) {
        println("stashed")
        stash()
      } else {
        isUpdating = true
        val curSender = sender()
        println(s"send query of $curSender at ${DateTime.now}")
        conn.postControl(queryParser.generate(append, schema)).map { result =>
          println(s"response to $curSender at ${DateTime.now}")
          curSender ! result
          self ! DataSetAgent.DoneUpdating
        }
      }
    case DataSetAgent.DoneUpdating =>
      isUpdating = false
      unstashAll()
    case _ =>
  }
}

object DataSetAgent {

  object DoneUpdating

  def props(schema: Schema, queryParser: IQLGenerator, conn: IDataConn)(implicit ec: ExecutionContext) =
    Props(new DataSetAgent(schema, queryParser, conn))
}
