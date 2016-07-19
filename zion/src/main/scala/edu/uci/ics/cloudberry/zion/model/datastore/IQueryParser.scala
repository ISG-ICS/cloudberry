package edu.uci.ics.cloudberry.zion.model.datastore

import edu.uci.ics.cloudberry.zion.model.schema.{Query, Schema}

trait IQueryParser {
  /**
    * Parser the Query to string statements. One query may produce multiple statements.
    *
    * @param query
    * @param schema
    * @return
    */
  def parse(query: Query, schema: Schema): Seq[String]

  protected def requireOrThrow(condition: Boolean, msg: String): Unit = {
    if (!condition) throw QueryParsingException(msg)
  }
}
