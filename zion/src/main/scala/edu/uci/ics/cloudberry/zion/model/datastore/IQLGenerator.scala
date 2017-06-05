package edu.uci.ics.cloudberry.zion.model.datastore

import edu.uci.ics.cloudberry.zion.model.schema.{IQuery, Query, AbstractSchema}

trait IQLGenerator {
  /**
    * Parser the Query to string statements.
    *
    * @param query
    * @param schemaMap
    * @return
    */
  def generate(query: IQuery, schemaMap: Map[String, AbstractSchema]): String

  def calcResultSchema(query: Query, schema: AbstractSchema): AbstractSchema

  protected def requireOrThrow(condition: Boolean, msg: String): Unit = {
    if (!condition) throw new QueryParsingException(msg)
  }
}

trait IQLGeneratorFactory {
  def apply(): IQLGenerator
}
