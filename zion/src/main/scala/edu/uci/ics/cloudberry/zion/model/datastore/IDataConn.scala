package edu.uci.ics.cloudberry.zion.model.datastore

import play.api.libs.json.JsValue
import play.api.libs.ws.WSResponse

import scala.concurrent.Future

trait IDataConn {

  def defaultQueryResponse: JsValue

  def postQuery(statement: String): Future[JsValue]

  def postControl(statement: String): Future[Boolean]

  def post(statement: String): Future[WSResponse]
}
