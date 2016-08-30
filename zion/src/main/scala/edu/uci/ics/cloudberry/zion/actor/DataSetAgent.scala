package edu.uci.ics.cloudberry.zion.actor

import akka.actor.{Actor, ActorLogging, Props, Stash}
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.schema.{AppendView, Query, Schema, UpsertRecord}

import scala.concurrent.ExecutionContext

class DataSetAgent(val schema: Schema, val queryParser: IQLGenerator, val conn: IDataConn)
                  (implicit ec: ExecutionContext)
  extends Actor with Stash with ActorLogging {

  def querying: Receive = {
    case query: Query =>
      val curSender = sender()
      conn.postQuery(queryParser.generate(query, schema)).map(curSender ! _)
  }

  override def receive: Receive = querying orElse {
    case append: AppendView =>
      process(queryParser.generate(append, schema))
    case upsert: UpsertRecord =>
      process(queryParser.generate(upsert, schema))
  }

  private def process(statement: String): Unit = {
    val curSender = sender()
    conn.postControl(statement).map { result =>
      curSender ! result
      self ! DataSetAgent.DoneUpdating
    }
    context.become(updating)
  }

  def updating: Receive = querying orElse {
    case append: AppendView => stash()
    case upsert: UpsertRecord => stash()
    case DataSetAgent.DoneUpdating =>
      unstashAll()
      context.unbecome()
  }

}

object DataSetAgent {

  object DoneUpdating

  def props(schema: Schema, queryParser: IQLGenerator, conn: IDataConn)(implicit ec: ExecutionContext) =
    Props(new DataSetAgent(schema, queryParser, conn))
}
