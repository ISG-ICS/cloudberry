package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import play.api.Logger
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

class AsterixAQLConn(url: String, wSClient: WSClient)(implicit ec: ExecutionContext) extends IDataConn {

  import AsterixAQLConn._

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
        Logger.error("AQL failed:" + Json.prettyPrint(wsResponse.json))
        failureHandler(wsResponse)
      }
    }
  }

  def post(aql: String): Future[WSResponse] = {
    Logger.debug("AQL:" + aql)
    val f = wSClient.url(url).withRequestTimeout(Duration.Inf).post(aql)
    f.onFailure(wsFailureHandler(aql))
    f
  }

  protected def wsFailureHandler(aql: String): PartialFunction[Throwable, Unit] = {
    case e: Throwable => Logger.error("WS Error:" + aql, e); throw e
  }
}

object AsterixAQLConn {
  val defaultEmptyResponse = Json.toJson(Seq(Seq.empty[JsValue]))
}

class AsterixSQLPPConn(url: String, wSClient: WSClient)(implicit ec: ExecutionContext) extends IDataConn {

  import AsterixSQLPPConn._

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
        Logger.error("Query failed:" + Json.prettyPrint(wsResponse.json))
        failureHandler(wsResponse)
      }
    }
  }

  def post(query: String): Future[WSResponse] = {
    Logger.debug("Query:" + query)
    val f = wSClient.url(url).withRequestTimeout(Duration.Inf).post(params(query))
    f.onFailure(wsFailureHandler(query))
    f
  }

  protected def wsFailureHandler(query: String): PartialFunction[Throwable, Unit] = {
    case e: Throwable => Logger.error("WS Error:" + query, e)
      throw e
  }

  protected def params(query: String): Map[String, Seq[String]] = {
    Map("statement" -> Seq(query), "mode" -> Seq("synchronous"), "include-results" -> Seq("true"))
  }
}

object AsterixSQLPPConn {
  val defaultEmptyResponse = Json.toJson(Seq(Seq.empty[JsValue]))
}
