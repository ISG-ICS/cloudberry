package edu.uci.ics.cloudberry.zion.model.actor

import akka.actor.{Actor, ActorLogging}
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQueryParser}
import edu.uci.ics.cloudberry.zion.model.schema.{AppendView, Query, Schema}

import scala.concurrent.ExecutionContext

class DataSetAgent(val schema: Schema, val queryParser: IQueryParser, val conn: IDataConn)
                  (implicit ec: ExecutionContext)
  extends Actor with ActorLogging {

  override def receive: Receive = {
    case query: Query =>
      val curSender = sender()
      conn.postQuery(queryParser.parse(query, schema)).map(curSender ! _)
    case append: AppendView =>
      val curSender = sender()
      conn.postControl(queryParser.parse(append, schema)).map(curSender ! _)
    case _ =>
  }
}
