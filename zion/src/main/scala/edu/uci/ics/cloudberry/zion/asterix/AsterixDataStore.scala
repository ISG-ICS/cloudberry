package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.zion.actor.DataStoreActor
import edu.uci.ics.cloudberry.zion.model.{DBQuery, DBSyncQuery, Response}
import play.api.libs.ws.WSResponse

import scala.concurrent.{ExecutionContext, Future}

abstract class AsterixDataStoreActor(override val name: String, val aqlConn: AsterixConnection)
                                    (implicit ec: ExecutionContext)
  extends DataStoreActor(name) {

  override def query(query: DBQuery): Future[Response] = {
    val aqlVisitor = new AQLVisitor(name)
    query.accept(aqlVisitor)
    val wsResponse = aqlConn.post(aqlVisitor.aqlBuilder.toString())
    wsResponse.map(handleWSResponse(_))
  }

  def handleWSResponse(wsResponse: WSResponse): Response
}
