package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.zion.api.{DBQuery, DBUpdateQuery, DataStore, Response}
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

abstract class AsterixDataStore(val name: String, val aqlConn: AsterixConnection) extends DataStore {

  override def query(query: DBQuery): Future[Response] = {
    val aqlVisitor = new AQLVisitor(this)
    query.accept(aqlVisitor)
    val wsResponse = aqlConn.post(aqlVisitor.aqlBuilder.toString())
    wsResponse.map(handleWSResponse(_))
  }

  override def update(query: DBUpdateQuery): Future[WSResponse] = ???

  def handleWSResponse(wsResponse: WSResponse): Response
}
