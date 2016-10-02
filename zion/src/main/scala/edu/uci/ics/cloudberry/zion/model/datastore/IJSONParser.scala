package edu.uci.ics.cloudberry.zion.model.datastore

import edu.uci.ics.cloudberry.zion.model.schema.{Query, QueryExeOptions}
import play.api.libs.json.JsValue

trait IJSONParser {
  def parse(json: JsValue): (Query, QueryExeOptions)
}
