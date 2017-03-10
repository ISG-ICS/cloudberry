package edu.uci.ics.cloudberry.zion.model.datastore

import edu.uci.ics.cloudberry.zion.model.schema.{Query, QueryExeOption, Schema}
import play.api.libs.json.JsValue

trait IJSONParser {

  def getDatasets(json: JsValue): Set[String]

  def parse(json: JsValue, schemaMap: Map[String, Schema]): (Seq[Query], QueryExeOption)
}
