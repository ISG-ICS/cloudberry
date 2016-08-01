package edu.uci.ics.cloudberry.zion.model.actor

import akka.actor.{Actor, ActorLogging}
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQueryParser, IResponse}
import edu.uci.ics.cloudberry.zion.model.schema.{AppendSelfQuery, Query, Schema}
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

class DataSetActor(val schema: Schema, val queryParser: IQueryParser, val conn: IDataConn) extends Actor with ActorLogging {

  def query(query: Query): Future[IResponse] = {
    conn.post(queryParser.parse(query, schema)).map(processResponse(_))
  }

  def processResponse(wsResponse: WSResponse): IResponse = ???

  override def receive: Receive = {
    case query: Query =>
      val curSender = sender()
      this.query(query).map(curSender ! _)
    case append: AppendSelfQuery =>
    case _ =>
  }
}
