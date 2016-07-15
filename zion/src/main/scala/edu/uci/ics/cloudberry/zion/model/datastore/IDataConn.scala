package edu.uci.ics.cloudberry.zion.model.datastore

import play.api.libs.ws.WSResponse

import scala.concurrent.Future

trait IDataConn {

  def query(statement: String): Future[WSResponse]
}
