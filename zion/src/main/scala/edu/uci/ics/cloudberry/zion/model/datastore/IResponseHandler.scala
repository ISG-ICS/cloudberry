package edu.uci.ics.cloudberry.zion.model.datastore

import play.api.libs.ws.WSResponse

trait IResponseHandler {
  def handler(wsResponse: WSResponse) : IResponse
}
