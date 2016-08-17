package edu.uci.ics.cloudberry.zion.model.datastore

import edu.uci.ics.cloudberry.zion.model.schema.{IQuery, Query, Schema}

trait IQLGenerator {
  /**
    * Parser the Query to string statements.
    *
    * @param query
    * @param schema
    * @return
    */
  def generate(query: IQuery, schema: Schema): String

  def calcResultSchema(query: Query, schema: Schema): Schema

  protected def requireOrThrow(condition: Boolean, msg: String): Unit = {
    if (!condition) throw new QueryParsingException(msg)
  }
}

trait IQLGeneratorFactory {
  def apply(): IQLGenerator
}
