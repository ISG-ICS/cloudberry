package edu.uci.ics.cloudberry.zion.model.actor

import akka.actor.{Actor, ActorLogging}
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQueryParser, IResponse}
import edu.uci.ics.cloudberry.zion.model.schema.{IQuery, Schema}
import play.api.libs.ws.WSResponse

import scala.concurrent.{ExecutionContext, Future}

class DataSetActor(val schema: Schema, val queryParser: IQueryParser, val conn: IDataConn)
                  (implicit ec: ExecutionContext)
  extends Actor with ActorLogging {

  def query(query: IQuery): Future[IResponse] = conn.post(queryParser.parse(query, schema)).map(processResponse)

  def processResponse(wsResponse: WSResponse): IResponse = ???

  override def receive: Receive = {
    case query: IQuery =>
      val curSender = sender()
      this.query(query).map(curSender ! _)
    case _ =>
  }
}
