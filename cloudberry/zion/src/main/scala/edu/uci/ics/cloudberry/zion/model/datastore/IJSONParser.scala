package edu.uci.ics.cloudberry.zion.model.datastore

import edu.uci.ics.cloudberry.zion.model.schema.{Query, QueryExeOption, AbstractSchema}
import play.api.libs.json.JsValue

trait IJSONParser {

  /**
    * returns all datasets access by the query
    * @param json
    * @return
    */
  def getDatasets(json: JsValue): Set[String]


  /**
    * parses the json object into a sequence of [[Query]] with [[QueryExeOption]]
    * @param json
    * @param schemaMap
    * @return
    */
  def parse(json: JsValue, schemaMap: Map[String, AbstractSchema]): (Seq[Query], QueryExeOption)
}
