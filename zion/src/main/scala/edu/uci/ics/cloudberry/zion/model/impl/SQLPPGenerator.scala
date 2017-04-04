package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{IQLGenerator, IQLGeneratorFactory, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema._
import play.api.libs.json.Json

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Provide constant query strings for SQL++
  */
object SQLPPAsterixImpl extends AsterixImpl {
  override val aggregateFuncMap: Map[AggregateFunc, String] = Map(
    Count -> "coll_count",
    Max -> "coll_max",
    Min -> "coll_min",
    Avg -> "coll_avg",
    Sum -> "coll_sum"
  )


  val datetime: String = "datetime"
  val round: String = "round"

  val dayTimeDuration: String = "day_time_duration"
  val yearMonthDuration: String = "year_month_duration"
  val getIntervalStartDatetime: String = "get_interval_start_datetime"
  val intervalBin: String = "interval_bin"

  val spatialIntersect: String = "spatial_intersect"
  val createRectangle: String = "create_rectangle"
  val createPoint: String = "create_point"
  val spatialCell: String = "spatial_cell"
  val getPoints: String = "get_points"

  val similarityJaccard: String = "similarity_jaccard"
  val contains: String = "contains"
  val wordTokens: String = "word_tokens"


}

class SQLPPGenerator extends AsterixQueryGenerator {

  protected val typeImpl: AsterixImpl = SQLPPAsterixImpl

  protected val sourceVar: String = "t"

  protected val unnestVar: String = "unnest"

  protected val lookupVar: String = "l"

  protected val groupVar: String = "g"

  protected val globalAggrVar: String = "c"

  protected val outerSelectVar: String = "s"

  protected val quote = "`"

  protected val suffix: String = ";"

  def parseCreate(create: CreateView, schemaMap: Map[String, Schema]): String = {
    val sourceSchema = schemaMap(create.query.dataset)
    val resultSchema = calcResultSchema(create.query, schemaMap(create.query.dataset))
    val ddl: String = genDDL(resultSchema)
    val createDataSet =
      s"""
         |drop dataset ${create.dataset} if exists;
         |create dataset ${create.dataset}(${resultSchema.typeName}) primary key ${resultSchema.primaryKey.map(_.name).mkString(",")} //with filter on '${resultSchema.timeField.name}'
         |""".stripMargin
    val insert =
      s"""
         |insert into ${create.dataset} (
         |${parseQuery(create.query, schemaMap)}
         |)""".stripMargin
    ddl + createDataSet + insert
  }

  def parseAppend(append: AppendView, schemaMap: Map[String, Schema]): String = {
    s"""
       |upsert into ${append.dataset} (
       |${parseQuery(append.query, schemaMap)}
       |)""".stripMargin
  }

  def parseUpsert(q: UpsertRecord, schemaMap: Map[String, Schema]): String = {
    s"""
       |upsert into ${q.dataset} (
       |${Json.toJson(q.records)}
       |)""".stripMargin
  }

  def parseQuery(query: Query, schemaMap: Map[String, Schema]): String = {

    val exprMap: Map[String, FieldExpr] = initExprMap(query, schemaMap)

    val resultAfterLookup = parseLookup(query.lookup, exprMap)
    val lookupStr = resultAfterLookup.strs(0)
    val fromStr = s"from ${query.dataset} $sourceVar $lookupStr".trim

    val resultAfterUnnest = parseUnnest(query.unnest, resultAfterLookup.exprMap)
    val unnestStr = resultAfterUnnest.strs(0)
    val unnestTests = resultAfterUnnest.strs.tail

    val resultAfterFilter = parseFilter(query.filter, resultAfterUnnest.exprMap, unnestTests)
    val filterStr = resultAfterFilter.strs(0)

    val resultAfterGroup = parseGroupby(query.groups, resultAfterFilter.exprMap)
    val groupSQL = resultAfterGroup.strs(0)

    val resultAfterSelect = parseSelect(query.select, resultAfterGroup.exprMap, query)
    val projectStr = resultAfterSelect.strs(0)
    val orderStr = resultAfterSelect.strs(1)
    val limitStr = resultAfterSelect.strs(2)
    val offsetStr = resultAfterSelect.strs(3)


    val queryStr = Seq(
      projectStr,
      fromStr,
      unnestStr,
      filterStr,
      groupSQL,
      orderStr,
      limitStr,
      offsetStr).filter(!_.isEmpty).mkString("\n")

    val resultAfterGlobalAggr = parseGlobalAggr(query.globalAggr, resultAfterSelect.exprMap, queryStr)
    resultAfterGlobalAggr.strs.head
  }


  private def parseLookup(lookups: Seq[LookupStatement],
                          exprMap: Map[String, FieldExpr]): ParsedResult = {
    val producedExprs = mutable.Map.newBuilder[String, FieldExpr]

    val lookupStr = lookups.zipWithIndex.map {
      case (lookup, id) =>
        val lookupExpr = s"l$id"
        val conditions = lookup.lookupKeys.zip(lookup.sourceKeys).map {
          case (lookupKey, sourceKey) =>
            val sourceExpr = exprMap(sourceKey.name)
            s"$lookupExpr.${lookupKey.name} = ${sourceExpr.refExpr}"
        }
        lookup.as.zip(lookup.selectValues).foreach {
          case (as, selectValue) =>
            val expr = s"$lookupExpr.$quote${selectValue.name}$quote"
            producedExprs += (as.name -> FieldExpr(expr, expr))
        }
        s"""left outer join ${lookup.dataset} $lookupExpr on ${conditions.mkString(" and ")}"""
    }.mkString("\n")

    ParsedResult(Seq(lookupStr), (producedExprs ++= exprMap).result().toMap)
  }

  private def parseFilter(filters: Seq[FilterStatement], exprMap: Map[String, FieldExpr], unnestTestStrs: Seq[String]): ParsedResult = {
    if (filters.isEmpty && unnestTestStrs.isEmpty) {
      ParsedResult(Seq(""), exprMap)
    } else {
      val filterStrs = filters.map { filter =>
        parseFilterRelation(filter, exprMap(filter.field.name).refExpr)
      }
      val filterStr = (unnestTestStrs ++ filterStrs).mkString("where ", " and ", "")

      ParsedResult(Seq(filterStr), exprMap)
    }
  }

  private def parseUnnest(unnest: Seq[UnnestStatement],
                          exprMap: Map[String, FieldExpr]): ParsedResult = {
    val producedExprs = mutable.Map.newBuilder[String, FieldExpr]
    val unnestTestStrs = new ListBuffer[String]
    val unnestStr = unnest.zipWithIndex.map {
      case (unnest, id) =>
        val expr = exprMap(unnest.field.name)
        val newExpr = s"${quote}unnest$id$quote"
        producedExprs += (unnest.as.name -> FieldExpr(newExpr, newExpr))
        if (unnest.field.isOptional) {
          unnestTestStrs += s"not(is_null(${expr.refExpr}))"
        }
        s"unnest ${expr.refExpr} $newExpr"
    }.mkString("\n")

    unnestTestStrs.prepend(unnestStr)
    ParsedResult(unnestTestStrs.toSeq, (producedExprs ++= exprMap).result().toMap)
  }

  private def parseGroupby(groupOpt: Option[GroupStatement],
                           exprMap: Map[String, FieldExpr]): ParsedResult = {
    groupOpt match {
      case Some(group) =>
        val producedExprs = mutable.Map.newBuilder[String, FieldExpr]
        val groupStrs = group.bys.map { by =>
          val fieldExpr = exprMap(by.field.name)
          val as = by.as.getOrElse(by.field)
          val groupExpr = parseGroupByFunc(by, fieldExpr.refExpr)
          val newExpr = s"$quote${as.name}$quote"
          producedExprs += (as.name -> FieldExpr(newExpr, newExpr))
          s"$groupExpr as $newExpr"
        }
        val groupStr = s"group by ${groupStrs.mkString(",")} group as $groupVar"

        group.aggregates.foreach { aggr =>
          val fieldExpr = exprMap(aggr.field.name)
          //def
          val aggrExpr = parseAggregateFunc(aggr, fieldExpr.refExpr)
          //ref
          val newExpr = s"$quote${aggr.as.name}$quote"
          producedExprs += aggr.as.name -> FieldExpr(newExpr, aggrExpr)
        }

        ParsedResult(Seq(groupStr), producedExprs.result().toMap)
      case None => ParsedResult(Seq(""), exprMap)
    }
  }


  private def parseSelect(selectOpt: Option[SelectStatement],
                          exprMap: Map[String, FieldExpr], query: Query): ParsedResult = {
    selectOpt match {
      case Some(select) =>
        val producedExprs = mutable.Map.newBuilder[String, FieldExpr]
        val orderStrs = select.orderOn.zip(select.order).map {
          case (orderOn, order) =>
            val expr = exprMap(orderOn.name).refExpr
            val orderStr = if (order == SortOrder.DSC) "desc" else ""
            s"${expr} $orderStr"
        }
        val orderStr = if (orderStrs.nonEmpty) {
          orderStrs.mkString("order by ", ",", "")
        } else {
          ""
        }
        val limitStr = s"limit ${select.limit}"
        val offsetStr = s"offset ${select.offset}"

        if (select.fields.isEmpty) {
          producedExprs ++= exprMap
        } else {
          select.fields.foreach {
            case AllField => producedExprs ++= exprMap
            case field => producedExprs += field.name -> exprMap(field.name)
          }
        }
        val newExprMap = producedExprs.result().toMap
        val projectStr = if (select.fields.isEmpty) {
          if (query.hasUnnest || query.hasGroup) {
            parseProject(exprMap)
          } else {
            s"select value $sourceVar"
          }
        } else {
          parseProject(newExprMap)
        }
        ParsedResult(Seq(projectStr, orderStr, limitStr, offsetStr), newExprMap)

      case None =>
        val projectStr =
          if (query.hasUnnest || query.hasGroup) {
            parseProject(exprMap)
          } else {
            s"select value $sourceVar"
          }
        ParsedResult(Seq(projectStr, "", "", ""), exprMap)
    }

  }

  private def parseProject(exprMap: Map[String, FieldExpr]): String = {
    exprMap.filter {
      case (field, expr) => field != "*" && expr.refExpr != sourceVar
    }.map {
      case (field, expr) => s"${expr.defExpr} as $quote$field$quote"
    }.mkString("select ", ",", "")
  }

  private def parseGlobalAggr(globalAggrOpt: Option[GlobalAggregateStatement],
                              exprMap: Map[String, FieldExpr],
                              queryStr: String): ParsedResult = {
    globalAggrOpt match {
      case Some(globalAggr) =>
        val producedExprs = mutable.Map.newBuilder[String, FieldExpr]
        val aggr = globalAggr.aggregate
        val funcName = typeImpl.getAggregateStr(aggr.func)

        val newDefExpr = if (aggr.func == Count) {
          globalAggrVar
        } else {
          s"${globalAggrVar}.$quote${aggr.field.name}$quote"
        }
        val newRefExpr = s"$quote${aggr.as.name}$quote"

        producedExprs += aggr.as.name -> FieldExpr(newRefExpr, newDefExpr)
        val result =
          s"""
             |select $funcName(
             |(select value $newDefExpr from ($queryStr) as $globalAggrVar)
             |) as $quote${aggr.as.name}$quote""".stripMargin
        ParsedResult(Seq(result), producedExprs.result().toMap)
      case None =>
        ParsedResult(Seq(queryStr), exprMap)
    }
  }

  protected def parseNumberRelation(filter: FilterStatement,
                                    fieldExpr: String): String = {
    filter.relation match {
      case Relation.inRange =>
        if (filter.values.size != 2) throw new QueryParsingException(s"relation: ${filter.relation} require two parameters")
        s"$fieldExpr >= ${filter.values(0)} and $fieldExpr < ${filter.values(1)}"
      case Relation.in =>
        s"$fieldExpr in [ ${filter.values.mkString(",")} ]"
      case _ =>
        s"$fieldExpr ${filter.values} ${filter.values.head}"
    }
  }

  protected def parseAggregateFunc(aggr: AggregateStatement,
                                   fieldExpr: String): String = {
    def aggFuncExpr(aggFunc: String): String = {
      if (aggr.field.name.equals("*")) {
        s"$aggFunc($groupVar)"
      } else {
        s"$aggFunc( (select value $groupVar.$fieldExpr from $groupVar) )"
      }
    }

    aggr.func match {
      case topK: TopK => ???
      case DistinctCount => ???
      case _ => aggFuncExpr(typeImpl.getAggregateStr(aggr.func))
    }
  }
}

object SQLPPGenerator extends IQLGeneratorFactory {
  override def apply(): IQLGenerator = new SQLPPGenerator()
}
