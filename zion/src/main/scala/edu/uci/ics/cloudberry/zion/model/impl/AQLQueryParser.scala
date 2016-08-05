package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{FieldNotFound, IQueryParser, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema._

import scala.collection.mutable

class AQLQueryParser extends IQueryParser {

  override def parse(query: IQuery, schema: Schema): String = {
    query match {
      case q: Query =>
        validateQuery(q)
        parseQuery(q, schema)
      case q: CreateView => parseCreate(q, schema)
      case q: AppendView => ???
      case q: DropView => ???
    }
  }

  //TODO combine with parseQuery
  def calcResultSchema(query: Query, schema: Schema): Schema = {
    if (query.lookup.isEmpty && query.groups.isEmpty && query.select.isEmpty) {
      schema.copy()
    } else {
      //TODO
      ???
    }
  }

  def parseCreate(create: CreateView, schema: Schema): String = {
    // generate schema
    // create data type by schema
    // drop statement
    // insert into
    ???
  }

  def parseQuery(query: Query, schema: Schema): String = {

    val sourceVar = "$t"
    val dataset = s"for $sourceVar in dataset ${query.dataset}"

    val schemaVars: Map[String, AQLVar] = schema.fieldMap.mapValues { f =>
      f.dataType match {
        case DataType.Record => AQLVar(f, sourceVar)
        case DataType.Hierarchy => AQLVar(f, sourceVar) // TODO rethink this type: a type or just a relation between types?
        case _ => {
          //Add the quote to wrap the name in order to not touch the AQL reserved keyword
          val addQuote = f.name.split('.').map(name => s"'$name'").mkString(".")
          AQLVar(f, s"$sourceVar.$addQuote")
        }
      }
    }

    val (lookup, varMapAfterLookup) = parseLookup(query.lookup, schemaVars)
    val filter = parseFilter(query.filter, varMapAfterLookup)
    val (unnest, varMapAfterUnnest) = parseUnnest(query.unnest, varMapAfterLookup)

    val groupVar = "$g"
    val (group, varMapAfterGroup) = query.groups.map(parseGroupby(_, varMapAfterUnnest, groupVar))
      .getOrElse(("", varMapAfterUnnest))

    val varName = if (group.length > 0) groupVar else sourceVar
    val (selectPrefix, select) = query.select.map(parseSelect(_, varMapAfterGroup, group.length > 0, varName))
      .getOrElse("", "")

    Seq(selectPrefix, dataset, lookup, filter, unnest, group, select).mkString("\n")
  }

  private def parseLookup(lookups: Seq[LookupStatement],
                          varMap: Map[String, AQLVar]
                         ): (String, Map[String, AQLVar]) = {
    val sb = StringBuilder.newBuilder
    val producedVar = mutable.Map.newBuilder[String, AQLVar]
    lookups.zipWithIndex.foreach { case (lookup, id) =>
      val lookupVar = s"$$l$id"
      val keyZip = lookup.lookupKeys.zip(lookup.sourceKeys).map { case (lookupKey, sourceKey) =>
        varMap.get(sourceKey) match {
          case Some(aqlVar) => s"$lookupVar.$lookupKey = ${aqlVar.aqlExpr}"
          case None => throw FieldNotFound(sourceKey)
        }
      }
      sb.append(
        s"""
           |for $lookupVar in dataset ${lookup.dataset}
           |where ${keyZip.mkString(" and ")}
        """.stripMargin
      )
      //TODO check if the vars are duplicated
      val field: Field = ??? // get field from lookup table
      producedVar ++= lookup.as.zip(lookup.selectValues).map { p =>
        p._1 -> AQLVar(new Field(p._1, field.dataType), s"$lookupVar.${p._2}")
      }
    }
    (sb.toString(), (producedVar ++= varMap).result().toMap)
  }

  private def parseFilter(filters: Seq[FilterStatement], varMap: Map[String, AQLVar]): String = {
    if (filters.isEmpty) return ""
    filters.map { filter =>
      varMap.get(filter.fieldName) match {
        case Some(variable) =>
          AQLFuncVisitor.translateRelation(variable.field, filter.funcOpt, variable.aqlExpr, filter.relation, filter.values)
        case None => throw FieldNotFound(filter.fieldName)
      }
    }.mkString("where ", " and ", "")
  }

  private def parseUnnest(unnest: Seq[UnnestStatement],
                          varMap: Map[String, AQLVar]
                         ): (String, Map[String, AQLVar]) = {
    val producedVar = mutable.Map.newBuilder[String, AQLVar]
    val aql = unnest.zipWithIndex.map { case (stat, id) =>
      varMap.get(stat.fieldName) match {
        case Some(aqlVar) =>
          aqlVar.field match {
            case field: BagField =>
              val newVar = s"$$unnest$id"
              producedVar += stat.as -> AQLVar(new Field(stat.as, field.innerType), newVar)
              //TODO test if this null check will introduce more db time
              s"""
                 |where not(is-null(${aqlVar.aqlExpr}))
                 |for $newVar in ${aqlVar.aqlExpr}
                 |""".stripMargin
            case _ => throw new QueryParsingException("unnest can only apply on Bag type")
          }
        case None => throw FieldNotFound(stat.fieldName)
      }
    }.mkString("\n")
    (aql, (producedVar ++= varMap).result().toMap)
  }

  private def parseGroupby(group: GroupStatement,
                           varMap: Map[String, AQLVar],
                           varGroupSource: String = "$g"
                          ): (String, Map[String, AQLVar]) = {
    val producedVar = mutable.Map.newBuilder[String, AQLVar]
    val groupByAQLPair: Seq[(String, String)] = group.bys.zipWithIndex.map { case (by, id) =>
      varMap.get(by.fieldName) match {
        case Some(aqlVar) =>
          val key = by.as.getOrElse(aqlVar.field.name)
          val varKey = s"$$g$id"
          val (dataType, aqlGrpFunc) = AQLFuncVisitor.translateGroupFunc(aqlVar.field, by.funcOpt, aqlVar.aqlExpr)
          producedVar += key -> AQLVar(new Field(key, dataType), s"$varGroupSource.$key")
          (s"$varKey := $aqlGrpFunc", s" '$key' : $varKey")
        case None => throw FieldNotFound(by.fieldName)
      }
    }

    //used to append to the AQL `group by`'s `with` clause to expose the required fields
    val letExpr = mutable.Seq.newBuilder[String]
    val aggrRequiredVar = mutable.Seq.newBuilder[String]
    val aggrNameMap = group.aggregates.map { aggr =>
      varMap.get(aggr.fieldName) match {
        case Some(aqlVar) =>
          val (dataType, aqlAggExpr, newvar, newvarexpr) = AQLFuncVisitor.translateAggrFunc(aqlVar.field, aggr.func, aqlVar.aqlExpr)
          aggrRequiredVar += newvar
          letExpr += newvarexpr
          producedVar += aggr.as -> AQLVar(new Field(aggr.as, dataType), s"$varGroupSource.${aggr.as}")
          s"'${aggr.as}' : $aqlAggExpr"
        case None => throw FieldNotFound(aggr.fieldName)
      }
    }

    val groups = groupByAQLPair.map(_._1).mkString(", ")
    val retGroups = groupByAQLPair.map(_._2)

    val aql =
      s"""
         |${letExpr.result().mkString(",")}
         |group by $groups with ${aggrRequiredVar.result().mkString(",")}
         |return {
         |  ${(retGroups ++ aggrNameMap).mkString(",")}
         |}
         |""".stripMargin
    (aql, producedVar.result.toMap)
  }

  private def parseSelect(select: SelectStatement,
                          varMap: Map[String, AQLVar],
                          isInGroup: Boolean,
                          sourceVar: String = "$g"
                         ): (String, String) = {

    val (prefix, wrap) = if (isInGroup) (s"for $sourceVar in (", ")") else ("", "")
    //sampling only
    val orders = select.orderOn.map { fieldNameWithOrder =>
      val order = if (fieldNameWithOrder.startsWith("-")) "desc" else ""
      val fieldName = if (fieldNameWithOrder.startsWith("-")) fieldNameWithOrder.substring(1) else fieldNameWithOrder
      varMap.get(fieldName) match {
        case Some(aqlVar) => s"${aqlVar.aqlExpr} $order"
        case None => throw FieldNotFound(fieldName)
      }
    }
    val ordersAQL = if (orders.nonEmpty) orders.mkString("order by ", ",", "") else ""

    val rets = select.fields.map { fieldName =>
      varMap.get(fieldName) match {
        case Some(aqlVar) => s" '${aqlVar.field.name}': ${aqlVar.aqlExpr}"
        case None => throw FieldNotFound(fieldName)
      }
    }
    val retAQL = if (rets.nonEmpty) rets.mkString("{", ",", "}") else sourceVar

    val aql =
      s"""
         |$wrap
         |$ordersAQL
         |limit ${select.limit}
         |offset ${select.offset}
         |return
         |$retAQL
         """.stripMargin
    (prefix, aql)
  }

  private def validateQuery(query: Query): Unit = {
    requireOrThrow(query.select.isDefined || query.groups.isDefined, "either group or select statement is required")
  }

  case class AQLVar(field: Field, aqlExpr: String)

}
