package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{IQueryParser, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema._

import scala.collection
import scala.collection.mutable

class AqlQueryParser extends IQueryParser {


  def validateQuery(query: Query): Unit = {
    if (query.order.isEmpty && query.groups.isEmpty) {
      throw QueryParsingException("either groupBy or orderBy statement is required")
    }
  }


  override def parse(query: Query, schema: Schema): Seq[String] = {
    validateQuery(query)
    val schemaVars = schema.getFieldNames
    val dataset = s"for $$t in dataset ${query.dataset}"
    val (lookup, lookupVarMap) = parseLookup(query.lookup)
    val filter = parseFilter(query.filter, schemaVars, lookupVarMap)
    val group = parseGroupby(query.groups)
    val order = parseOrder(query.order)
    Seq(Seq(dataset, lookup, filter, group, order).mkString("\n"))
  }

  private def parseLookup(lookups: Seq[LookupStatement]): (String, Map[String, String]) = {
    val sb = StringBuilder.newBuilder
    val mapBuilder = mutable.Map.newBuilder[String, String]
    lookups.zipWithIndex.foreach { case (lookup, id) =>
      val sourceVar = "$t"
      val lookupVar = s"$$l$id"
      val keyZip = lookup.lookupKeys.zip(lookup.sourceKeys).map { case (lookupKey: String, sourceKey: String) =>
        s"$lookupVar.$lookupKey = $sourceVar.$sourceKey"
      }
      sb.append(
        s"""
           |for $$l$id in dataset ${lookup.dataset}
           |where ${keyZip.mkString(" and ")}
        """.stripMargin
      )
      //TODO check if the vars duplicated
      mapBuilder ++= lookup.as.zip(lookup.selectValues).map(p => p._1 -> p._2)
    }
    (sb.toString(), mapBuilder.result().toMap)
  }

  private def parseFilter(filters: Seq[FilterStatement], schemaVars: Seq[String], lookupVarMap: Map[String, String]): String = ???

  private def parseGroupby(groups: Option[GroupStatement]): String = ???

  private def parseOrder(order: Option[OrderStatement]): String = ???
}
