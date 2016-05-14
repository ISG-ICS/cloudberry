package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.util.Logging
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

class AQLStatement() {

}

object AQLStatement {

}


class AsterixConnection(wSClient: WSClient, url: String)(implicit ec: ExecutionContext) extends Logging {

  def post(aql: String): Future[WSResponse] = {
    log.info("AQL:" + aql)
    val f = wSClient.url(url).withRequestTimeout(Duration.Inf).post(aql)
    f.onFailure(failureHandler(aql))
    f
  }

  protected def failureHandler(aql: String): PartialFunction[Throwable, Unit] = {
    case e: Throwable => log.error("WS Error:" + aql, e); throw e
  }
}
