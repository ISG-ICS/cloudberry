package edu.uci.ics.cloudberry.zion.model.datastore

import play.api.libs.json.JsArray

trait IResponse {

}

case class QueryResponse(jsArray: JsArray) extends IResponse

case class ControlResponse(info: String) extends IResponse
