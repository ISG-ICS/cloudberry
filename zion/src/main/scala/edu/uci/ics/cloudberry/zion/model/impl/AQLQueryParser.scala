package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{IQueryParser, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema.DataType.DataType
import edu.uci.ics.cloudberry.zion.model.schema._

import scala.collection.mutable

class AQLQueryParser extends IQueryParser {

  class ProducedField(origField: Field, func: IFunction, as: String, dataType: DataType) extends Field(as, dataType)

  val sourceVar = "$t"


  override def parse(query: Query, schema: Schema): Seq[String] = {
    validateQuery(query)
    val schemaVars = schema.getFieldNames
    val dataset = s"for $sourceVar in dataset ${query.dataset}"
    val (lookup, lookupVarMap) = parseLookup(query.lookup)
    val filter = parseFilter(query.filter, schemaVars, lookupVarMap, schema)
    val group = query.groups.map(parseGroupby(_, schemaVars, lookupVarMap, schema)).getOrElse("")
    val order = query.select.map(parseSelect(_)).getOrElse("")
    Seq(Seq(dataset, lookup, filter, group, order).mkString("\n"))
  }

  private def parseLookup(lookups: Seq[LookupStatement]): (String, Map[String, String]) = {
    val sb = StringBuilder.newBuilder
    val mapBuilder = mutable.Map.newBuilder[String, String]
    val productedVar = mutable.Map.newBuilder[String, ProducedField]
    lookups.zipWithIndex.foreach { case (lookup, id) =>
      val lookupVar = s"$$l$id"
      val keyZip = lookup.lookupKeys.zip(lookup.sourceKeys).map { case (lookupKey, sourceKey) =>
        s"$lookupVar.$lookupKey = $sourceVar.$sourceKey"
      }
      sb.append(
        s"""
           |for $$l$id in dataset ${lookup.dataset}
           |where ${keyZip.mkString(" and ")}
        """.stripMargin
      )
      //TODO check if the vars are duplicated
      mapBuilder ++= lookup.as.zip(lookup.selectValues).map(p => p._1 -> p._2)
    }
    (sb.toString(), mapBuilder.result().toMap)
  }

  private def parseFilter(filters: Seq[FilterStatement],
                          schemaVars: Seq[String],
                          lookupVarMap: Map[String, String],
                          schema: Schema
                         ): String = {
    filters.map { filter =>
      requireOrThrow(schemaVars.contains(filter.fieldName) || lookupVarMap.contains(filter.fieldName),
                     s"cannot find field: ${filter.fieldName}")
      //TODO check the lookup table's schema
      schema.getField(filter.fieldName).map { field =>
        AQLFuncVisitor.translateRelation(field, filter.funcOpt, sourceVar, filter.relation, filter.values)
      }.getOrElse(throw QueryParsingException(s"cannot find field: ${filter.fieldName} in table, lookup haven't implemented"))
    }.mkString("where ", "\n and ", "")
  }

  private def parseGroupby(group: GroupStatement,
                           schemaVars: Seq[String],
                           lookupVarMap: Map[String, String],
                           schema: Schema
                          ): String = {
    //TODO the produced the new var could be used by select potentially
    val productedVar = mutable.Map.newBuilder[String, ProducedField]
    val groupNameMap = group.bys.flatMap { by =>
      schema.getField(by.fieldName).map { field =>
        by.as.getOrElse(field.name) -> AQLFuncVisitor.translateGroupFunc(field, by.funcOpt, sourceVar)
      }.orElse(throw QueryParsingException(s"cannot find field: ${by.fieldName}"))
    }

    val aggrNameMap = group.aggregates.flatMap { aggr =>
      if (aggr.func == Count) {
        Some(aggr.as -> s"count(${sourceVar})")
      } else {
        schema.getField(aggr.fieldName).map { field =>
          aggr.as -> AQLFuncVisitor.translateAggrFunc(field, Some(aggr.func), sourceVar)
        }.orElse(throw QueryParsingException(s"cannot find field: ${aggr.fieldName}"))
      }
    }

    val groups = groupNameMap.zipWithIndex.map { case ((as, field), id) =>
      s" $$g${id} := $field "
    }.mkString(" ")

    val retGroups = groupNameMap.zipWithIndex.map { case ((as, field), id) =>
      s"'$as' : $$g${id}"
    }

    val retAggrs = aggrNameMap.map { case (as, field) =>
      s"'$as' : $field"
    }

    s"""
       |group by $groups with ${sourceVar}
       |return {
       |  ${(retGroups ++ retAggrs).mkString(",")}
       |}
       |""".stripMargin
  }

  private def parseSelect(select: SelectStatement): String = {
    ???
  }

  private def validateQuery(query: Query): Unit = {
    requireOrThrow(query.select.isDefined || query.groups.isDefined, "either group or select statement is required")
  }

}
