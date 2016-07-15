package edu.uci.ics.cloudberry.zion.model.datastore

import edu.uci.ics.cloudberry.zion.model.schema.{Query, Schema}

trait IQueryParser {
  def parse(query: Query, schema: Schema): String
}
