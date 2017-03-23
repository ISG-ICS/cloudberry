package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{IQLGenerator, IQLGeneratorFactory, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema._
import play.api.libs.json.Json

import scala.collection.mutable


/**
  * Provide query constants for AQL
  */
object AQLTypeImpl extends TypeImpl {
  val aggregateFuncMap: Map[AggregateFunc, String] = Map(
    Count -> "count",
    Max -> "max",
    Min -> "min",
    Avg -> "avg",
    Sum -> "sum"
  )

  val dayTimeDuration: String = "day-time-duration"
  val yearMonthDuration: String = "year-month-duration"
  val getIntervalStartDatetime: String = "get-interval-start-datetime"
  val intervalBin: String = "interval-bin"

  val spatialIntersect: String = "spatial-intersect"
  val createRectangle: String = "create-rectangle"
  val createPoint: String = "create-point"
  val spatialCell: String = "spatial-cell"
  val getPoints: String = "get-points"

  val similarityJaccard: String = "similarity-jaccard"
  val contains: String = "contains"
  val wordTokens: String = "word-tokens"

}


class AQLGenerator extends AsterixQueryGenerator {

  protected val typeImpl: TypeImpl = AQLTypeImpl

  protected val sourceVar: String = "$t"

  protected val lookupVar: String = "$l"

  protected val unnestVar: String = "$unnest"

  protected val groupVar: String = "$g"

  protected val globalAggrVar: String = "$c"

  protected val outerSelectVar: String = "$s"

  protected val quote = "'"

  protected val suffix = ""


  def parseCreate(create: CreateView, schemaMap: Map[String, Schema]): String = {
    val resultSchema = calcResultSchema(create.query, schemaMap)
    val ddl: String = genDDL(resultSchema)
    val createDataSet =
      s"""
         |drop dataset ${create.dataset} if exists;
         |create dataset ${create.dataset}(${resultSchema.typeName}) primary key ${resultSchema.primaryKey.map(_.name).mkString(",")} //with filter on '${resultSchema.timeField.name}'
         |""".stripMargin
    val insert =
      s"""
         |insert into dataset ${create.dataset} (
         |${parseQuery(create.query, schemaMap)}
         |)
       """.stripMargin
    ddl + createDataSet + insert
  }

  def parseAppend(append: AppendView, schemaMap: Map[String, Schema]): String = {
    val sourceSchema = schemaMap(append.query.dataset)
    s"""
       |upsert into dataset ${append.dataset} (
       |${parseQuery(append.query, schemaMap)}
       |)
     """.stripMargin
  }

  def parseUpsert(q: UpsertRecord, schemaMap: Map[String, Schema]): String = {
    s"""
       |upsert into dataset ${q.dataset} (
       |${Json.toJson(q.records)}
       |)
     """.stripMargin
  }

  def parseQuery(query: Query, schemaMap: Map[String, Schema]): String = {

    val dataset = s"for $sourceVar in dataset ${query.dataset}"
    val exprMap = initExprMap(query, schemaMap)

    val resultAfterLookup = parseLookup(query.lookup, exprMap, schemaMap, sourceVar)

    val lookupStr = resultAfterLookup.strs(1)

    val resultAfterFilter = parseFilter(query.filter, resultAfterLookup.exprMap, resultAfterLookup.strs.head)
    val filterStr = resultAfterFilter.strs(1)

    val resultAfterUnnest = parseUnnest(query.unnest, resultAfterFilter.exprMap, resultAfterFilter.strs.head)
    val unnestStr = resultAfterUnnest.strs(1)

    val resultAfterGroup = parseGroupby(query.group, resultAfterUnnest.exprMap, resultAfterUnnest.strs.head)
    val groupStr = resultAfterGroup.strs(1)

    var resultAfterSelect = parseSelect(query.select, resultAfterGroup.exprMap, query, resultAfterGroup.strs.head)
    val selectPrefix = resultAfterSelect.strs(1)
    val selectStr = resultAfterSelect.strs(2)

    val resultAfterGlobalAggr = parseGlobalAggr(query.globalAggr, resultAfterSelect.exprMap, resultAfterSelect.strs.head)

    val globalAggrPrefix = resultAfterGlobalAggr.strs(1)
    val globalAggrRetStr = resultAfterGlobalAggr.strs(2)

    Seq(globalAggrPrefix, selectPrefix, dataset, lookupStr, filterStr, unnestStr, groupStr, selectStr, globalAggrRetStr).mkString("\n")
  }

  /** Returns a map of schema variables after appending lookup variables
    *
    * Maps lookup variable name to [[FieldExpr]] for every variable in every lookup statement and then appends it to
    * the variable map of the schema.
    * The lookup variable will be a sub-query to the lookup dataset.
    * Note: Since sub-query will return a list of the same value, we pick only the first value from the list using [0].
    *
    * @param lookups   Sequence of [[LookupStatement]] which contains lookup variables
    * @param exprMap   Map of variables in the dataset schema
    * @param schemaMap Map of dataset names to their schemas including lookup dataset schemas
    * @return exprMap after adding lookup variables
    */
  private def parseLookup(lookups: Seq[LookupStatement],
                          exprMap: Map[String, FieldExpr],
                          schemaMap: Map[String, Schema],
                          currentVar: String
                         ): PartialResult = {
    if (lookups.isEmpty) {
      return PartialResult(Seq(currentVar, ""), exprMap)
    }

    val lookupExprs = mutable.Map.newBuilder[String, FieldExpr]
    val producedExprs = mutable.Map.newBuilder[String, FieldExpr]
    exprMap.foreach {
      case (name, expr) =>
        val refExpr = s"$lookupVar.$quote$name$quote"
        lookupExprs += name -> FieldExpr(refExpr, expr.defExpr)
        producedExprs += name -> FieldExpr(refExpr, refExpr)
    }

    lookups.zipWithIndex.foreach { case (lookup, id) =>
      val localLookupVar = s"$lookupVar$id"
      val conditions = lookup.lookupKeys.zip(lookup.sourceKeys).map { case (lookupKey, sourceKey) =>
        val expr = exprMap(sourceKey.name)
        s"${expr.refExpr} /* +indexnl */ = $localLookupVar.${lookupKey.name}"
      }

      lookup.selectValues.zip(lookup.as).foreach {
        case (select, as) =>
          val defExpr =
            s"""
               |(for $localLookupVar in dataset ${lookup.dataset}
               |where ${conditions.mkString(" and ")}
               |return $localLookupVar.$quote${select.name}$quote)[0]
            """.stripMargin.trim
          val refExpr = s"$lookupVar.$quote${as.name}$quote"
          lookupExprs += as.name -> FieldExpr(refExpr, defExpr)
          producedExprs += as.name -> FieldExpr(refExpr, refExpr)
      }
    }

    val lookupStr = s"let $lookupVar := ${parseReturn(lookupExprs.result().toMap)}"
    PartialResult(Seq(lookupVar, lookupStr), producedExprs.result().toMap)
  }

  private def parseFilter(filters: Seq[FilterStatement], exprMap: Map[String, FieldExpr], currentVar: String): PartialResult = {
    if (filters.isEmpty) {
      return PartialResult(Seq(currentVar, ""), exprMap)
    }
    val filterStr = filters.map { filter =>
      val expr = exprMap(filter.field.name)
      parseFilterRelation(filter, expr.refExpr)
    }.mkString("where ", " and ", "")
    return PartialResult(Seq(currentVar, filterStr), exprMap)
  }

  private def parseUnnest(unnest: Seq[UnnestStatement],
                          exprMap: Map[String, FieldExpr], currentVar: String): PartialResult = {
    val producedVars = mutable.Map.newBuilder[String, FieldExpr]
    val unnestStr = unnest.zipWithIndex.map {
      case (unnest, id) =>
        val expr = exprMap(unnest.field.name)
        val field = unnest.field
        var localUnnestVar = s"$unnestVar$id"
        producedVars += unnest.as.name -> FieldExpr(localUnnestVar, localUnnestVar)
        s"""
           |${if (field.isOptional) s"where not(is-null(${expr.refExpr}))"}
           |for $localUnnestVar in ${expr.refExpr}
           |""".stripMargin
    }.mkString("\n")
    PartialResult(Seq(currentVar, unnestStr), (producedVars ++= exprMap).result().toMap)
  }

  private def parseGroupby(groupOpt: Option[GroupStatement],
                           exprMap: Map[String, FieldExpr], currentVar: String): PartialResult = {
    if (groupOpt.isEmpty) {
      return PartialResult(Seq(currentVar, ""), exprMap)
    }

    val producedVars = mutable.Map.newBuilder[String, FieldExpr]

    val group = groupOpt.get
    val groupStr = group.bys.zipWithIndex.map {
      case (by, id) =>
        val expr = exprMap(by.field.name)
        val newName = by.as.getOrElse(by.field).name
        val localGroupVar = s"$groupVar$id"
        val groupFuncExpr = parseGroupByFunc(by, expr.refExpr)
        producedVars += newName -> FieldExpr(localGroupVar, localGroupVar)
        s"$localGroupVar := $groupFuncExpr"
    }.mkString(", ")

    //used to append to the AQL `group by`'s `with` clause to expose the required fields
    val letExprs = mutable.Seq.newBuilder[String]
    val aggrRequiredVars = mutable.Seq.newBuilder[String]
    group.aggregates.foreach { aggr =>
      val expr = exprMap(aggr.field.name)
      val aggrExpr = parseAggregateFunc(aggr, expr.refExpr)
      val newVar = getAggrFieldVar(aggr.field, expr.refExpr)
      val letExpr = s"let $newVar := ${expr.refExpr}"
      letExprs += letExpr
      aggrRequiredVars += newVar
      producedVars += aggr.as.name -> FieldExpr(s"$groupVar.$quote${aggr.as.name}$quote", aggrExpr)
    }

    val newExprMap = producedVars.result().toMap
    val queryStr =
      s"""
         |${letExprs.result().mkString(",")}
         |group by $groupStr with ${aggrRequiredVars.result().mkString(",")}
         |return ${parseReturn(newExprMap)}
         |""".stripMargin

    PartialResult(Seq(groupVar, queryStr), newExprMap)
  }

  private def parseSelect(selectOpt: Option[SelectStatement],
                          exprMap: Map[String, FieldExpr],
                          query: Query,
                          currentVar: String): PartialResult = {

    if (selectOpt.isEmpty) {
      if (query.grouped) {
        return PartialResult(Seq(globalAggrVar, "", ""), exprMap)
      } else {
        if (query.unnested) {
          return PartialResult(Seq(globalAggrVar, "", s"return ${parseReturn(exprMap)}"), exprMap)
        } else {
          return PartialResult(Seq(globalAggrVar, "", s"return $sourceVar"), exprMap)
        }
      }
    }
    val select = selectOpt.get
    val producedVars = mutable.Map.newBuilder[String, FieldExpr]

    val (nextVar, prefix, wrap) = if (query.grouped) {
      (outerSelectVar, s"for $currentVar in (", ")")
    }
    else {
      (globalAggrVar, "", "")
    }
    //sampling only
    val orderStrs = select.orderOn.zip(select.order).map { case (orderOn, order) =>
      val expr = exprMap(orderOn.name)
      val orderStr = if (order == SortOrder.DSC) "desc" else ""
      s"${expr.refExpr} $orderStr"
    }
    val orderStr = if (orderStrs.nonEmpty) {
      orderStrs.mkString("order by ", ",", "")
    } else {
      ""
    }

    if (select.fields.isEmpty) {
      producedVars ++= exprMap
    } else {
      select.fields.foreach {
        case AllField =>
          producedVars ++= exprMap
        case field =>
          producedVars += field.name -> exprMap(field.name)
      }
    }

    val newExprMap = producedVars.result().toMap
    val retStr = if (select.fields.nonEmpty || (query.unnested && !query.grouped)) {
      s"return ${parseReturn(newExprMap)}"
    } else {
      s"return $currentVar"
    }

    val queryStr =
      s"""
         |$wrap
         |$orderStr
         |limit ${select.limit}
         |offset ${select.offset}
         |$retStr
         """.stripMargin
    PartialResult(Seq(nextVar, prefix, queryStr), newExprMap)
  }

  /**
    *
    * @param globalAggrOpt
    * @param exprMap
    * @return String: Prefix containing AQL statement for the aggr function. e.g.: count( for $c in (
    *         String: wrap and return the prefix statement . e.g.: ) return $c )
    *         Map[String, AQLVar]: result variables map after aggregation.
    */
  private def parseGlobalAggr(globalAggrOpt: Option[GlobalAggregateStatement],
                              exprMap: Map[String, FieldExpr], currentVar: String): PartialResult = {
    if (globalAggrOpt.isEmpty) {
      return PartialResult(Seq("", "", ""), exprMap)
    }

    val (forPrefix, forWrap) = (s"for $currentVar in (", ")")
    val producedVars = mutable.Map.newBuilder[String, FieldExpr]
    val aggr = globalAggrOpt.get.aggregate

    val expr = exprMap(aggr.field.name)
    val funcName = typeImpl.getAggregateStr(aggr.func)
    val returnVar = if (aggr.func == Count) {
      currentVar
    } else {
      s"$currentVar.$quote${aggr.field.name}$quote"
    }

    val prefixStr =
      s"""
         |{'${aggr.as.name}': ${funcName}(
         |$forPrefix
         """.stripMargin

    val returnStr =
      s"""
         |$forWrap
         |return $returnVar
         |)
         |}
         |""".stripMargin


    return PartialResult(Seq("", prefixStr, returnStr), producedVars.result().toMap)
  }

  protected def parseNumberRelation(filter: FilterStatement, aqlExpr: String) = {
    filter.relation match {
      case Relation.inRange =>
        s"$aqlExpr >= ${
          filter.values(0)
        } and $aqlExpr < ${
          filter.values(1)
        }"
      case Relation.in =>
        val setVar = s"$$set${
          filter.field.name.replace('.', '_')
        }"
        s"""|true
            |for $setVar in [ ${
          filter.values.mkString(",")
        } ]
            |where $aqlExpr = $setVar
            |""".stripMargin
      case _ =>
        s"$aqlExpr ${filter.relation} ${filter.values.head}"
    }
  }

  protected def parseReturn(exprMap: Map[String, FieldExpr]): String = {
    val str = exprMap.filter(e => e._1 != "*" && e._2.refExpr != sourceVar).map {
      case (name, expr) => s"""'$name' : ${expr.defExpr}"""
    }.mkString(",")
    s"""{
       |  $str
       |}
    """.stripMargin
  }


  protected def parseAggregateFunc(aggr: AggregateStatement,
                                   fieldExpr: String): String = {
    val newExpr = getAggrFieldVar(aggr.field, fieldExpr)
    aggr.func match {
      case _: TopK => ???
      case DistinctCount => ???
      case _ =>
        s"${typeImpl.getAggregateStr(aggr.func)}($newExpr)"
    }
  }

  protected def getAggrFieldVar(aggrField: Field, fieldExpr: String): String = {
    if (aggrField.name == "*") {
      s"${fieldExpr.split('.')(0)}aggr"
    } else {
      s"$$${aggrField.name}_aggr"
    }
  }


  protected def getGlobalAggrVar(query: Query): String = {
    if (query.selected) {
      outerSelectVar
    } else {
      globalAggrVar
    }
  }

}

object AQLGenerator extends IQLGeneratorFactory {
  override def apply(): IQLGenerator = new AQLGenerator()
}