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
