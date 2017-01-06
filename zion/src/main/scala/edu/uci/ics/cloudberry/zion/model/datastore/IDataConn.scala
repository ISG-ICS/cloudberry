package edu.uci.ics.cloudberry.zion.model.datastore

import edu.uci.ics.cloudberry.util.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

trait IDataConn {

  def defaultQueryResponse: JsValue

  def postQuery(statement: String): Future[JsValue]

  def postControl(statement: String): Future[Boolean]

  def post(statement: String): Future[WSResponse]
}

class AsterixConn(url: String, wSClient: WSClient)(implicit ec: ExecutionContext) extends IDataConn with Logging {

  import AsterixConn._

  override def defaultQueryResponse: JsValue = defaultEmptyResponse

  def postQuery(aql: String): Future[JsValue] = {
    postWithCheckingStatus(aql, (ws: WSResponse) => ws.json, (ws: WSResponse) => defaultQueryResponse)
  }

  def postControl(aql: String): Future[Boolean] = {
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

  def post(aql: String): Future[WSResponse] = {
    log.debug("AQL:" + aql)
    val f = wSClient.url(url).withRequestTimeout(Duration.Inf).post(aql)
    f.onFailure(wsFailureHandler(aql))
    f
  }

  protected def wsFailureHandler(aql: String): PartialFunction[Throwable, Unit] = {
    case e: Throwable => log.error("WS Error:" + aql, e); throw e
  }
}

object AsterixConn {
  val defaultEmptyResponse = Json.toJson(Seq(Seq.empty[JsValue]))
}
