package edu.uci.ics.cloudberry.zion.model.datastore

import edu.uci.ics.cloudberry.zion.model.schema.{IQuery, Query, Schema}

trait IQueryParser {
  /**
    * Parser the Query to string statements.
    *
    * @param query
    * @param schema
    * @return
    */
  def parse(query: IQuery, schema: Schema): String

  protected def requireOrThrow(condition: Boolean, msg: String): Unit = {
    if (!condition) throw new QueryParsingException(msg)
  }
}
