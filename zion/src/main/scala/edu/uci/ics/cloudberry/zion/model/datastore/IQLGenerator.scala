package edu.uci.ics.cloudberry.zion.model.datastore

import edu.uci.ics.cloudberry.zion.model.schema.{IQuery, Query, Schema}

trait IQLGenerator {
  /**
    * Parser the Query to string statements.
    *
    * @param query
    * @param schemaMap
    * @return
    */
  def generate(query: IQuery, schemaMap: Map[String, Schema]): String

  def calcResultSchema(query: Query, schemaMap: Map[String, Schema]): Schema

}

trait IQLGeneratorFactory {
  def apply(): IQLGenerator
}
