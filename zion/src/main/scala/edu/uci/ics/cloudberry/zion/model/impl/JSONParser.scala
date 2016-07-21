package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.IJSONParser
import edu.uci.ics.cloudberry.zion.model.schema.Query
import play.api.libs.json.JsValue

class JSONParser extends IJSONParser{
  override def parse(json: JsValue): Query = ???
}
