package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, Props, Stash}
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.schema.{AppendView, Query, Schema}
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext

class DataSetAgent(val schema: Schema, val queryParser: IQLGenerator, val conn: IDataConn)
                  (implicit ec: ExecutionContext)
  extends Actor with Stash with ActorLogging {

  def querying: Receive = {
    case query: Query =>
      val curSender = sender()
      conn.postQuery(queryParser.generate(query, schema)).map(curSender ! _)
  }

  def normal: Receive = querying orElse {
    case append: AppendView =>
      val curSender = sender()
      println(s"send query of $curSender at ${DateTime.now}")
      conn.postControl(queryParser.generate(append, schema)).map { result =>
        println(s"response to $curSender at ${DateTime.now}")
        curSender ! result
        self ! DataSetAgent.DoneUpdating
      }
      context.become(updating)
  }

  def updating: Receive = querying orElse {
    case append: AppendView => stash()
    case DataSetAgent.DoneUpdating =>
      unstashAll()
      context.become(normal)
  }

  override def receive: Receive = normal
}

object DataSetAgent {

  object DoneUpdating

  def props(schema: Schema, queryParser: IQLGenerator, conn: IDataConn)(implicit ec: ExecutionContext) =
    Props(new DataSetAgent(schema, queryParser, conn))
}
