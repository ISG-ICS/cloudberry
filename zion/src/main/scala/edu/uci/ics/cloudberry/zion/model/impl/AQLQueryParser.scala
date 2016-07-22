package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{IQueryParser, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema.DataType.DataType
import edu.uci.ics.cloudberry.zion.model.schema._

import scala.collection.mutable

class AQLQueryParser extends IQueryParser {

  case object AllField extends Field("*", DataType.Bag)

  case class ProducedField(val origField: Field, val as: String, val aqlVar: String, override val dataType: DataType) extends Field(as, dataType)

  val sourceVar = "$t"


  override def parse(query: Query, schema: Schema): Seq[String] = {
    validateQuery(query)
    val schemaVars = schema.getFieldNames
    val dataset = s"for $sourceVar in dataset ${query.dataset}"
    val (lookup, lookupVarMap) = parseLookup(query.lookup)
    val filter = parseFilter(query.filter, schemaVars, lookupVarMap, schema)
    val (unnest, unnestMap) = parseUnnest(query.unnest, schemaVars, lookupVarMap, schema)
    val (group, groupMap) = query.groups.map(parseGroupby(_, schemaVars, lookupVarMap, unnestMap, schema)).getOrElse("")
    val (selectPrefix, selectSuffix) = query.select.map(parseSelect(_, lookupVarMap, unnestMap, groupMap, schema)).getOrElse(("", ""))
    Seq(Seq(selectPrefix, dataset, lookup, filter, unnest, group, selectSuffix).mkString("\n"))
  }

  private def parseLookup(lookups: Seq[LookupStatement]): (String, Map[String, ProducedField]) = {
    val sb = StringBuilder.newBuilder
    val producedVar = mutable.Map.newBuilder[String, ProducedField]
    lookups.zipWithIndex.foreach { case (lookup, id) =>
      val lookupVar = s"$$l$id"
      val keyZip = lookup.lookupKeys.zip(lookup.sourceKeys).map { case (lookupKey, sourceKey) =>
        s"$lookupVar.$lookupKey = $sourceVar.$sourceKey"
      }
      sb.append(
        s"""
           |for $lookupVar in dataset ${lookup.dataset}
           |where ${keyZip.mkString(" and ")}
        """.stripMargin
      )
      //TODO check if the vars are duplicated
      val field: Field = ??? // get field from lookup table
      producedVar ++= lookup.as.zip(lookup.selectValues).map(p => p._1 -> ProducedField(field, p._1, s"$lookupVar.${p._2}", field.dataType))
    }
    (sb.toString(), producedVar.result().toMap)
  }

  private def parseFilter(filters: Seq[FilterStatement],
                          schemaVars: Seq[String],
                          lookupVarMap: Map[String, ProducedField],
                          schema: Schema
                         ): String = {
    filters.map { filter =>
      requireOrThrow(schemaVars.contains(filter.fieldName) || lookupVarMap.contains(filter.fieldName),
                     s"cannot find field: ${filter.fieldName}")
      //TODO check the lookup table's schema
      schema.getField(filter.fieldName).map { field =>
        AQLFuncVisitor.translateRelation(field, filter.funcOpt, sourceVar, filter.relation, filter.values)
      }.getOrElse(throw QueryParsingException(s"cannot find field: ${filter.fieldName} in table, lookup haven't implemented"))
    }.mkString("where ", " and ", "")
  }

  private def parseUnnest(unnest: Seq[UnnestStatement],
                          schemaVars: Seq[String],
                          lookupVarMap: Map[String, ProducedField],
                          schema: Schema): (String, Map[String, ProducedField]) = {
    val producedVar = mutable.Map.newBuilder[String, ProducedField]
    val aql = unnest.zipWithIndex.map { case (stat, id) =>
      requireOrThrow(schemaVars.contains(stat.fieldName), s"cannot find field: ${stat.fieldName}")
      schema.getField(stat.fieldName).map { field =>
        requireOrThrow(field.dataType == DataType.Bag, s"unnest can only apply on Bag type")
        val aqlVar = s"$$unnest$id"
        producedVar += stat.as -> ProducedField(field, stat.as, aqlVar, field.asInstanceOf[BagField].innerType)
        //TODO maybe this null check will introduce more AQL operations
        s"""
           |where not(is-null($sourceVar.${field.name}))
           |for $aqlVar in $sourceVar.${field.name}
           |""".stripMargin
      }.getOrElse(throw QueryParsingException(s"cannot find field: ${stat.fieldName} in table, lookup haven't implemented"))
    }.mkString("\n")
    (aql, producedVar.result().toMap)
  }

  private def parseGroupby(group: GroupStatement,
                           schemaVars: Seq[String],
                           lookupVarMap: Map[String, ProducedField],
                           unnestVarMap: Map[String, ProducedField],
                           schema: Schema
                          ): (String, Map[String, ProducedField]) = {
    //TODO the produced the new var could be used by select potentially
    val productedVar = mutable.Map.newBuilder[String, ProducedField]
    val groupNameMap = group.bys.flatMap { by =>
      schema.getField(by.fieldName).orElse(unnestVarMap.get(by.fieldName)).map { field =>
        val varName = if (field.isInstanceOf[ProducedField]) {
          field.asInstanceOf[ProducedField].aqlVar
        } else {
          sourceVar
        }

        val key = by.as.getOrElse(field.name)
        productedVar += (key -> ProducedField(field, key, key, field.dataType))
        by.as.getOrElse(field.name) -> AQLFuncVisitor.translateGroupFunc(field, by.funcOpt, varName)
      }.orElse {
        throw QueryParsingException(s"cannot find field: ${by.fieldName}")
      }
    }

    val aggrNameMap = group.aggregates.flatMap { aggr =>
      if (aggr.func == Count) {
        productedVar += (aggr.as -> ProducedField(AllField, aggr.as, aggr.as, DataType.Bag))
        Some(aggr.as -> s"count(${sourceVar})")
      } else {
        schema.getField(aggr.fieldName).map { field =>
          productedVar += (aggr.as -> ProducedField(field, aggr.as, aggr.as, field.dataType))
          aggr.as -> AQLFuncVisitor.translateAggrFunc(field, Some(aggr.func), sourceVar)
        }.orElse(throw QueryParsingException(s"cannot find field: ${aggr.fieldName}"))
      }
    }

    val groups = groupNameMap.zipWithIndex.map { case ((as, field), id) =>
      s" $$g${id} := $field "
    }.mkString(", ")

    val retGroups = groupNameMap.zipWithIndex.map { case ((as, field), id) =>
      s"'$as' : $$g${id}"
    }

    val retAggrs = aggrNameMap.map { case (as, field) =>
      s"'$as' : $field"
    }

    val aql =
      s"""
         |group by $groups with ${sourceVar}
         |return {
         |  ${(retGroups ++ retAggrs).mkString(",")}
         |}
         |""".stripMargin
    (aql, productedVar.result.toMap)
  }

  private def parseSelect(select: SelectStatement,
                          lookupVarMap: Map[String, ProducedField],
                          unnestVarMap: Map[String, ProducedField],
                          groupVarMap: Map[String, ProducedField],
                          schema: Schema
                         ): (String, String) = {
    val selVar = if (groupVarMap.isEmpty) sourceVar else "$sel"

    if (groupVarMap.isEmpty) {
      //sampling only
      val orders = select.orderOn.map { fieldNameWithOrder =>
        val order = if (fieldNameWithOrder.startsWith("-")) "desc" else ""
        val fieldName = if (fieldNameWithOrder.startsWith("-")) fieldNameWithOrder.substring(1) else fieldNameWithOrder
        val field = schema.getField(fieldName).getOrElse(findField(fieldName, lookupVarMap, unnestVarMap))
        field match {
          case f: ProducedField => s"${f.aqlVar}.$fieldName $order"
          case f: Field => s"${selVar}.$fieldName $order"
        }
      }
      val ordersAQL = if (orders.size > 0) orders.mkString("order by ", ",", "") else ""

      val retAQL = select.fields.map { fieldName =>
        val field = schema.getField(fieldName).getOrElse(findField(fieldName, lookupVarMap, unnestVarMap))
        field match {
          case f: ProducedField => s"'${f.as}':${f.aqlVar}"
          case f: Field => s"'$fieldName':$selVar.$fieldName"
        }
      }

      val aql =
        s"""
           |$ordersAQL
           |limit ${select.limit}
           |offset ${select.offset}
           |return {
           |${retAQL.mkString(",")}
           |}
         """.stripMargin
      ("", aql)
    } else {
      val prefix = s"for $selVar in ("
      val orders = select.orderOn.map { fieldNameWithOrder =>
        val order = if (fieldNameWithOrder.startsWith("-")) "desc" else ""
        val fieldName = if (fieldNameWithOrder.startsWith("-")) fieldNameWithOrder.substring(1) else fieldNameWithOrder
        val field = findField(fieldName, groupVarMap)
        field match {
          case f: ProducedField => s"$selVar.$fieldName $order"
          case f: Field => throw QueryParsingException("not possible")
        }
      }
      val ordersAQL = if (orders.size > 0) orders.mkString("order by ", ",", "") else ""

      val retAQL = select.fields.map { fieldName =>
        val field = findField(fieldName, groupVarMap)
        field match {
          case f: ProducedField => s"'$selVar':${f.as}"
          case f: Field => throw QueryParsingException("not possible")
        }
      }

      val suffix =
        s"""
           |)
           |$ordersAQL
           |limit ${select.limit}
           |offset ${select.offset}
           |return {
           |${retAQL.mkString(",")}
           |}
       """.stripMargin
      (prefix, suffix)
    }
  }

  private def validateQuery(query: Query): Unit = {
    requireOrThrow(query.select.isDefined || query.groups.isDefined, "either group or select statement is required")
  }

  private def findField(fieldName: String, maps: Map[String, ProducedField]*): Field = {
    maps match {
      case map :: Nil => map.getOrElse(fieldName, throw QueryParsingException(s"field ${fieldName} not found"))
      case map :: tail => map.getOrElse(fieldName, findField(fieldName, tail: _*))
    }
  }

}
