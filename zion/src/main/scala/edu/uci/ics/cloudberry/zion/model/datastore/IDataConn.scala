package edu.uci.ics.cloudberry.zion.model.datastore

import edu.uci.ics.cloudberry.util.Logging
import play.api.Logger
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
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

  def postQuery(query: String): Future[JsValue] = {
    postWithCheckingStatus(query, (ws: WSResponse) => {
      ws.json.asInstanceOf[JsObject].value("results")
    }, (ws: WSResponse) => defaultQueryResponse)
  }

  def postControl(query: String): Future[Boolean] = {
    postWithCheckingStatus(query, (ws: WSResponse) => true, (ws: WSResponse) => false)
  }

  protected def postWithCheckingStatus[T](query: String, succeedHandler: WSResponse => T, failureHandler: WSResponse => T): Future[T] = {
    post(query).map { wsResponse =>
      if (wsResponse.json.asInstanceOf[JsObject].value.get("status") == Some(JsString("success"))) {
        succeedHandler(wsResponse)
      }
      else {
        log.error("Query failed:" + Json.prettyPrint(wsResponse.json))
        failureHandler(wsResponse)
      }
    }
  }

  def post(query: String): Future[WSResponse] = {
    log.debug("Query:" + query)
    val f = wSClient.url(url).withRequestTimeout(Duration.Inf).post(params(query))
    f.onFailure(wsFailureHandler(query))
    f
  }

  protected def wsFailureHandler(query: String): PartialFunction[Throwable, Unit] = {
    case e: Throwable => log.error("WS Error:" + query, e)
      throw e
  }

  protected def params(query: String): Map[String, Seq[String]] = {
    Map("statement" -> Seq(query), "mode" -> Seq("synchronous"), "include-results" -> Seq("true"))
  }
}

object AsterixConn {
  val defaultEmptyResponse = Json.toJson(Seq(Seq.empty[JsValue]))
}
