package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{IQLGenerator, QueryParsingException, QueryResolver}
import edu.uci.ics.cloudberry.zion.model.schema._


trait TypeImpl {
  val aggregateFuncMap: Map[AggregateFunc, String]

  def apply(aggregate: AggregateFunc): String = {
    aggregateFuncMap.get(aggregate) match {
      case Some(impl) => impl
      case None => throw new QueryParsingException(s"No implementation is provided for aggregate function ${aggregate.name}")
    }
  }

}

abstract class AbstractQueryGenerator extends IQLGenerator {

  val resolver = new QueryResolver

  /**
    * Returns a query string query after parsing the query object.
    *
    * @param query     [[IQuery]] object containing query details
    * @param schemaMap a map of Dataset name to it's [[Schema]]
    * @return query string
    **/
  def generate(query: IQuery, schemaMap: Map[String, Schema]): String = {
    resolver.analyze(query, schemaMap)
    val result = query match {
      case q: Query =>
        parseQuery(q, schemaMap)
      case q: CreateView => parseCreate(q, schemaMap)
      case q: AppendView => parseAppend(q, schemaMap)
      case q: UpsertRecord => parseUpsert(q, schemaMap)
      case q: DropView => ???
      case _ => ???
    }
    s"$result$suffix"
  }

  protected def typeImpl: TypeImpl

  protected def parseQuery(query: Query, schemaMap: Map[String, Schema]): String

  protected def parseCreate(query: CreateView, schemaMap: Map[String, Schema]): String

  protected def parseAppend(query: AppendView, schemaMap: Map[String, Schema]): String

  protected def parseUpsert(query: UpsertRecord, schemaMap: Map[String, Schema]): String

  protected def suffix: String

}
