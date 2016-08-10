package edu.uci.ics.cloudberry.zion.asterix

import edu.uci.ics.cloudberry.util.Logging
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import play.api.libs.json.{JsArray, JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.parsing.json.JSONObject

class AQLStatement() {

}

object AQLStatement {

}


class AsterixConnection(url: String, wSClient: WSClient, config: Config)(implicit ec: ExecutionContext) extends Logging {

  import AsterixConnection._

  def postQuery(aql: String, responseWhenFail: JsValue = defaultEmptyResponse): Future[JsValue] = {
    postWithCheckingStatus(aql, (ws: WSResponse) => ws.json, (ws: WSResponse) => responseWhenFail)
  }

  def postUpdate(aql: String): Future[Boolean] = {
    postWithCheckingStatus(aql, (ws: WSResponse) => true, (ws: WSResponse) => false)
  }

  protected def postWithCheckingStatus[T](aql: String, succeedHandler: WSResponse => T, failureHandler: WSResponse => T): Future[T] = {
    post(aql).map { wsResponse =>
      if (wsResponse.status == 200) {
        succeedHandler(wsResponse)
      } else {
        log.error("AQL failed:" + Json.prettyPrint(wsResponse.json))
        failureHandler(wsResponse)
      }
    }
  }

  protected def post(aql: String): Future[WSResponse] = {
    log.info("AQL:" + aql)
    val f = wSClient.url(url).withRequestTimeout(Duration.Inf).post(aql)
    f.onFailure(wsFailureHandler(aql))
    f
  }

  protected def wsFailureHandler(aql: String): PartialFunction[Throwable, Unit] = {
    case e: Throwable => log.error("WS Error:" + aql, e); throw e
  }
}

object AsterixConnection {
  val defaultEmptyResponse = Json.toJson(Seq(Seq.empty[JsValue]))
}
