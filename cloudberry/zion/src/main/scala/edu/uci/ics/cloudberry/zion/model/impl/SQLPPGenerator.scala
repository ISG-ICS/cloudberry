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
  val fullTextContains: String = "ftcontains"
  val contains: String = "contains"
  val wordTokens: String = "word_tokens"


}

class SQLPPGenerator extends AsterixQueryGenerator {

  protected val typeImpl: AsterixImpl = SQLPPAsterixImpl

  protected val sourceVar: String = "t"

  protected val appendVar: String = "ta"

  protected val unnestVar: String = "unnest"

  protected val lookupVar: String = "l"

  protected val groupVar: String = "g"

  protected val groupedLookupVar: String = "ll"

  protected val groupedLookupSourceVar: String = "tt"

  protected val globalAggrVar: String = "c"

  protected val outerSelectVar: String = "s"

  protected val quote = "`"

  protected val suffix: String = ";"

  def parseCreate(create: CreateView, schemaMap: Map[String, Schema]): String = {
    val sourceSchema = schemaMap(create.query.dataset)
    val resultSchema = calcResultSchema(create.query, sourceSchema)
    val ddl: String = genDDL(resultSchema)
    val createDataSet =
      s"""
         |drop dataset ${create.dataset} if exists;
         |create dataset ${create.dataset}(${resultSchema.typeName}) primary key ${resultSchema.primaryKey.map(_.name).mkString(",")}; //with filter on '${resultSchema.timeField.name}'
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

  def parseUpsert(q: UpsertRecord, schemaMap: Map[String, AbstractSchema]): String = {
    s"""
       |upsert into ${q.dataset} (
       |${Json.toJson(q.records)}
       |)""".stripMargin
  }

  protected def parseDelete(delete: DeleteRecord, schemaMap: Map[String, AbstractSchema]): String = {
    if (delete.filters.isEmpty) {
      throw new QueryParsingException("Filter condition is required for DeleteRecord query.")
    }
    val exprMap: Map[String, FieldExpr] = initExprMap(delete.dataset, schemaMap)
    val queryBuilder = new StringBuilder()
    queryBuilder.append(s"delete from ${delete.dataset} $sourceVar")
    parseFilter(delete.filters, exprMap, Seq.empty, queryBuilder)
    queryBuilder.toString
  }

  protected def parseDrop(query: DropView, schemaMap: Map[String, AbstractSchema]): String = {
    s"drop dataset ${query.dataset} if exists"
  }


  def parseQuery(query: Query, schemaMap: Map[String, Schema]): String = {
    val queryBuilder = new mutable.StringBuilder()

    val exprMap: Map[String, FieldExpr] = initExprMap(query.dataset, schemaMap)
    val fromStr = s"from ${query.dataset} $sourceVar".trim
    queryBuilder.append(fromStr)

    val resultAfterLookup = parseLookup(query.lookup, exprMap, queryBuilder, false)

    val resultAfterUnnest = parseUnnest(query.unnest, resultAfterLookup.exprMap, queryBuilder)
    val unnestTests = resultAfterUnnest.strs

    val resultAfterFilter = parseFilter(query.filter, resultAfterUnnest.exprMap, unnestTests, queryBuilder)

    val resultAfterAppend = parseAppend(query.append, resultAfterFilter.exprMap, queryBuilder)

    val resultAfterGroup = parseGroupby(query.groups, resultAfterAppend.exprMap, queryBuilder)

    val resultAfterSelect = parseSelect(query.select, resultAfterGroup.exprMap, query, queryBuilder)

    val resultAfterGlobalAggr = parseGlobalAggr(query.globalAggr, resultAfterSelect.exprMap, queryBuilder)
    queryBuilder.toString
  }

  private def parseAppend(appends: Seq[AppendStatement], exprMap: Map[String, FieldExpr], queryBuilder: StringBuilder): ParsedResult = {
    if (appends.isEmpty) {
      ParsedResult(Seq.empty, exprMap)
    } else {
      val producedExprs = mutable.LinkedHashMap.newBuilder[String, FieldExpr]
      
      appends.foreach { append =>
        val as = append.as
        producedExprs += append.as.name -> FieldExpr(s"$appendVar.$quote${as.name}$quote", append.definition)
      }
      val producedExprMap = producedExprs.result().toMap
      val selectStr = parseProject(producedExprMap)+s",$sourceVar" 
      
      val newExprMap = producedExprMap ++ exprMap.mapValues{expr=>
        FieldExpr(s"$appendVar.${expr.refExpr}", s"${expr.defExpr}")
      }
      queryBuilder.insert(0, s"from ($selectStr\n")
      queryBuilder.append(s") $appendVar")
      ParsedResult(Seq.empty, newExprMap)
    }


  }


  private def parseLookup(lookups: Seq[LookupStatement],
                          exprMap: Map[String, FieldExpr],
                          queryBuilder: StringBuilder,
                          inGroup: Boolean): ParsedResult = {
    //use LinkedHashMap to preserve the order of fields
    val producedExprs = mutable.LinkedHashMap.newBuilder[String, FieldExpr]
    producedExprs ++= exprMap
    val actualLookupVar = if (inGroup) groupedLookupVar else lookupVar
    val lookupStr = lookups.zipWithIndex.map {
      case (lookup, id) =>
        val lookupExpr = s"$actualLookupVar$id"
        val conditions = lookup.lookupKeys.zip(lookup.sourceKeys).map {
          case (lookupKey, sourceKey) =>
            val sourceExpr = exprMap(sourceKey.name)
            s"$lookupExpr.$quote${lookupKey.name}$quote = ${sourceExpr.refExpr}"
        }
        lookup.as.zip(lookup.selectValues).foreach {
          case (as, selectValue) =>
            val expr = s"$lookupExpr.$quote${selectValue.name}$quote"
            producedExprs += (as.name -> FieldExpr(expr, expr))
        }
        s"""left outer join ${lookup.dataset} $lookupExpr on ${conditions.mkString(" and ")}"""
    }.mkString("\n")
    appendIfNotEmpty(queryBuilder, lookupStr)

    ParsedResult(Seq.empty, (producedExprs).result().toMap)
  }

  private def parseFilter(filters: Seq[FilterStatement], exprMap: Map[String, FieldExpr], unnestTestStrs: Seq[String], queryBuilder: StringBuilder): ParsedResult = {
    if (filters.isEmpty && unnestTestStrs.isEmpty) {
      ParsedResult(Seq.empty, exprMap)
    } else {
      val filterStrs = filters.map { filter =>
        parseFilterRelation(filter, exprMap(filter.field.name).refExpr)
      }
      val filterStr = (unnestTestStrs ++ filterStrs).mkString("where ", " and ", "")
      appendIfNotEmpty(queryBuilder, filterStr)

      ParsedResult(Seq.empty, exprMap)
    }
  }

  private def parseUnnest(unnest: Seq[UnnestStatement],
                          exprMap: Map[String, FieldExpr], queryBuilder: StringBuilder): ParsedResult = {
    val producedExprs = mutable.LinkedHashMap.newBuilder[String, FieldExpr]
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
    appendIfNotEmpty(queryBuilder, unnestStr)

    ParsedResult(unnestTestStrs.toSeq, (producedExprs ++= exprMap).result().toMap)
  }

  private def parseGroupby(groupOpt: Option[GroupStatement],
                           exprMap: Map[String, FieldExpr],
                           queryBuilder: StringBuilder): ParsedResult = {
    groupOpt match {
      case Some(group) =>
        val producedExprs = mutable.LinkedHashMap.newBuilder[String, FieldExpr]
        val groupStrs = group.bys.map { by =>
          val fieldExpr = exprMap(by.field.name)
          val as = by.as.getOrElse(by.field)
          val groupExpr = parseGroupByFunc(by, fieldExpr.refExpr)
          val newExpr = s"$quote${as.name}$quote"
          producedExprs += (as.name -> FieldExpr(newExpr, newExpr))
          s"$groupExpr as $newExpr"
        }
        val groupStr = s"group by ${groupStrs.mkString(",")} group as $groupVar"
        appendIfNotEmpty(queryBuilder, groupStr)

        group.aggregates.foreach { aggr =>
          val fieldExpr = exprMap(aggr.field.name)
          //def
          val aggrExpr = parseAggregateFunc(aggr, fieldExpr.refExpr)
          //ref
          val newExpr = s"$quote${aggr.as.name}$quote"
          producedExprs += aggr.as.name -> FieldExpr(newExpr, aggrExpr)
        }
        if (!group.lookups.isEmpty) {
          //we need to update producedExprs
          val producedExprMap = producedExprs.result().toMap
          val newExprMap =
            producedExprMap.map {
              case (field, expr) => field -> FieldExpr(s"$groupedLookupSourceVar.$quote$field$quote", s"$groupedLookupSourceVar.$quote$field$quote")
            }
          queryBuilder.insert(0, s"from (\n${parseProject(producedExprMap)}\n")
          queryBuilder.append(s"\n) $groupedLookupSourceVar\n")
          val resultAfterLookup = parseLookup(group.lookups, newExprMap, queryBuilder, true)
          ParsedResult(Seq.empty, resultAfterLookup.exprMap)
        } else {
          ParsedResult(Seq.empty, producedExprs.result().toMap)
        }


      case None => ParsedResult(Seq(""), exprMap)
    }
  }


  private def parseSelect(selectOpt: Option[SelectStatement],
                          exprMap: Map[String, FieldExpr], query: Query,
                          queryBuilder: StringBuilder): ParsedResult = {
    selectOpt match {
      case Some(select) =>
        val producedExprs = mutable.LinkedHashMap.newBuilder[String, FieldExpr]
        val orderStrs = select.orderOn.zip(select.order).map {
          case (orderOn, order) =>
            val expr = exprMap(orderOn.name).refExpr
            val orderStr = if (order == SortOrder.DSC) "desc" else ""
            s"${expr} $orderStr"
        }
        val orderStr =
          if (!orderStrs.isEmpty) {
            orderStrs.mkString("order by ", ",", "")
          } else {
            ""
          }

        val limitStr = s"limit ${select.limit}"
        val offsetStr = s"offset ${select.offset}"
        appendIfNotEmpty(queryBuilder, orderStr)
        appendIfNotEmpty(queryBuilder, limitStr)
        appendIfNotEmpty(queryBuilder, offsetStr)

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
        queryBuilder.insert(0, projectStr + "\n")
        ParsedResult(Seq.empty, newExprMap)

      case None =>
        val projectStr =
          if (query.hasUnnest || query.hasGroup) {
            parseProject(exprMap)
          } else {
            s"select value $sourceVar"
          }
        queryBuilder.insert(0, projectStr + "\n")
        ParsedResult(Seq.empty, exprMap)
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
                              queryBuilder: StringBuilder): ParsedResult = {
    globalAggrOpt match {
      case Some(globalAggr) =>
        val producedExprs = mutable.LinkedHashMap.newBuilder[String, FieldExpr]
        val aggr = globalAggr.aggregate
        val funcName = typeImpl.getAggregateStr(aggr.func)

        val newDefExpr = if (aggr.func == Count) {
          globalAggrVar
        } else {
          s"${globalAggrVar}.$quote${aggr.field.name}$quote"
        }
        val newRefExpr = s"$quote${aggr.as.name}$quote"

        producedExprs += aggr.as.name -> FieldExpr(newRefExpr, newDefExpr)
        val prepend =
          s"""
             |select $funcName(
             |(select value $newDefExpr from (""".stripMargin
        val append =
          s""") as $globalAggrVar)
             |) as $quote${aggr.as.name}$quote""".stripMargin
        queryBuilder.insert(0, prepend)
        queryBuilder.append(append)
        ParsedResult(Seq.empty, producedExprs.result().toMap)
      case None =>
        ParsedResult(Seq.empty, exprMap)
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
        s"$fieldExpr ${filter.relation} ${filter.values(0)}"
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

  /**
    * Append a new line and queryStr to the queryBuilder if queryStr is not empty.
    * Sometimes the generated queryStr could be empty, e.g., an empty sequence of [[FilterStatement]] or [[SelectStatement]],
    * which should not be appended to the queryBuilder.
    *
    * @param queryBuilder
    * @param queryStr
    */
  protected def appendIfNotEmpty(queryBuilder: StringBuilder, queryStr: String): Unit = {
    if (!queryStr.isEmpty) {
      queryBuilder.append("\n")
      queryBuilder.append(queryStr)
    }
  }
}

object SQLPPGenerator extends IQLGeneratorFactory {
  override def apply(): IQLGenerator = new SQLPPGenerator()
}
