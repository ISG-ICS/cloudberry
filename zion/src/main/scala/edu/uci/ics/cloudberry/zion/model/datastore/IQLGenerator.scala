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

  def generate(query: IQuery, schema: Schema): String = {
    generate(query, Map(query.dataset -> schema))
  }

  def calcResultSchema(query: Query, schema: Schema): Schema = {
    calcResultSchema(query, Map(query.dataset -> schema))
  }


}

trait IQLGeneratorFactory {
  def apply(): IQLGenerator
}
