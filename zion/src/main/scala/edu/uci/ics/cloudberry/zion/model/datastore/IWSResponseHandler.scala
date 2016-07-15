package edu.uci.ics.cloudberry.zion.model.datastore

import play.api.libs.ws.WSResponse

trait IWSResponseHandler {
  def handler(wsResponse: WSResponse) : IResponse
}
