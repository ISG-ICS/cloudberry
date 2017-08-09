package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{IQLGenerator, IQLGeneratorFactory, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.DateTime
import play.api.libs.json._

import scala.collection.mutable
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

class SQLGenerator extends IQLGenerator {

  /**
    * represent the expression for a [[Field]]
    *
    * @param refExpr the expression for referring this field by the subsequent statements
    * @param defExpr the expression the defines the field
    *
    */
  case class FieldExpr(refExpr: String, defExpr: String)

  /**
    * Partial parse results after parsing each [[Statement]]
    *
    * @param strs    a sequence of parsed query strings, which would be composed together later.
    * @param exprMap a new field expression map
    */
  case class ParsedResult(strs: Seq[String], exprMap: Map[String, FieldExpr])

  protected val sourceVar: String = "t"
  protected val appendVar: String = "ta"
  protected val unnestVar: String = "unnest"
  protected val lookupVar: String = "l"
  protected val allFieldVar: String = "*"
  protected val groupedLookupVar: String = "ll"
  protected val groupedLookupSourceVar: String = "tt"
  protected val globalAggrVar: String = "*"

  protected val quote = "`"

  protected val round: String = "round"
  protected val stringContains: String = "like"
  protected val fullTextMatch = Seq("match", "against")
  //a number truncated to a certain number of decimal places
  protected val truncate: String = "truncate"
  //converts a value in internal geometry format to its plain text representation, e.g.: "POINT(1, 2)"
  protected val geoAsText: String = "st_astext"
  //X/Y-coordinate value for the Point object.
  protected val pointGetCoord = Seq("st_x", "st_y")

  protected val aggregateFuncMap: Map[AggregateFunc, String] = Map(
    Count -> "count",
    Max -> "max",
    Min -> "min",
    Avg -> "avg",
    Sum -> "sum"
  )

  protected def timeUnitFuncMap(unit: TimeUnit.Value): String = unit match {
    case TimeUnit.Second => "second"
    case TimeUnit.Minute => "minute"
    case TimeUnit.Hour => "hour"
    case TimeUnit.Day => "date"
    case TimeUnit.Month => "month"
    case TimeUnit.Year => "year"
    case _ => throw new QueryParsingException(s"No implementation is provided for timeunit function ${unit.toString}")
  }

  protected def getAggregateStr(aggregate: AggregateFunc): String = {
    aggregateFuncMap.get(aggregate) match {
      case Some(impl) =>
        impl
      case None =>
        throw new QueryParsingException(s"No implementation is provided for aggregate function ${aggregate.name}")
    }
  }

  def generate(query: IQuery, schemaMap: Map[String, AbstractSchema]): String = {
    val result = query match {
      case q: Query => parseQuery(q, schemaMap)
      case q: CreateView => parseCreate(q, schemaMap)
      case q: AppendView => parseAppend(q, schemaMap)
      case q: UpsertRecord => parseUpsert(q)
      case q: DropView => parseDrop(q, schemaMap)
      case q: DeleteRecord => parseDelete(q, schemaMap)
      case _ => ???
    }
    s"$result"
  }

  def parseCreate(create: CreateView, schemaMap: Map[String, AbstractSchema]): String = {
    val (temporalSchemaMap, lookupSchemaMap) = GeneratorUtil.splitSchemaMap(schemaMap)
    val sourceSchema = temporalSchemaMap(create.query.dataset)
    val resultSchema = calcResultSchema(create.query, sourceSchema)
    val ddl: String = genDDL(create.dataset, sourceSchema)
    val insert =
      s"""
     |replace into $quote${create.dataset}$quote
     |(
     |${parseQuery(create.query, schemaMap)}
     |)
      """.stripMargin
    ddl + insert
  }

  protected def genDDL(name: String, schema: Schema): String = {
    def mkNestDDL(names: String, typeStr: String): String = {
      names match {
        case e => s"  $quote$e$quote $typeStr"
      }
    }

    val fields = schema.fieldMap.values.filter(f => f.dataType != DataType.Hierarchy && f != AllField).map {
      f => mkNestDDL(f.name, fieldType2MySQLType(f) + (if (f.isOptional) " default null" else " not null"))
    }
    s"""
       |create table if not exists $quote${name}$quote (
       |${fields.mkString(",\n")}, primary key (${schema.primaryKey.map(key => key.name).mkString("`","`,`","`")})
       |);
      """.stripMargin
  }

  /**
    * Convert middleware datatype to MySQL datatype
    * @param field
    */
  protected def fieldType2MySQLType(field: Field): String = {
    field.dataType match {
      case DataType.Number => "bigint"
      case DataType.Time => "datetime"
      case DataType.Point => "point"
      case DataType.Boolean => "tinyint"
      case DataType.String => "varchar(255)"
      case DataType.Text => "text"
      case DataType.Bag => ???
      case DataType.Hierarchy => ???
      case DataType.Record => ???
    }
  }

  def parseAppend(append: AppendView, schemaMap: Map[String, AbstractSchema]): String = {
    s"""
       |replace into $quote${append.dataset}$quote (
       |${parseQuery(append.query, schemaMap)}
       |)""".stripMargin
  }

  def parseUpsert(q: UpsertRecord): String = {
    q.dataset.equals(SQLConn.metaName) match {
      case true => parseUpsertMeta(q)
      case _ => ??? //TODO: general upsert
    }
  }

  def parseUpsertMeta(q: UpsertRecord): String = {
    val records = q.records.value
    var queryResult = ArrayBuffer.empty[String]
    records.foreach {
      record =>
        val name: String = (record \ "name").as[JsString].value
        val schema: JsValue = (record \ "schema").as[JsValue]
        val dataInterval: JsValue = (record \ "dataInterval").as[JsValue]
        val stats: JsValue = (record \ "stats").as[JsValue]
        val createTime: String = TimeField.TimeFormatForSQL.print(new DateTime((record \ "stats" \ "createTime").as[String]))
        queryResult += (s"('${name}','${schema}','${dataInterval}','${stats}','${createTime}')")
    }
    s"""
       |replace into $quote${q.dataset}$quote (`name`, `schema`, `dataInterval`, `stats`, `stats.createTime`) values
       |${queryResult.mkString(",")}
       |""".stripMargin
  }

  protected def parseDelete(delete: DeleteRecord, schemaMap: Map[String, AbstractSchema]): String = {
    if (delete.filters.isEmpty) {
      throw new QueryParsingException("Filter condition is required for DeleteRecord query.")
    }
    val exprMap: Map[String, FieldExpr] = initExprMap(delete.dataset, schemaMap)
    val queryBuilder = new StringBuilder()
    queryBuilder.append(s"delete from $quote${delete.dataset}$quote $sourceVar")
    parseFilter(delete.filters, exprMap, Seq.empty, queryBuilder)
    queryBuilder.toString()
  }

  protected def parseDrop(query: DropView, schemaMap: Map[String, AbstractSchema]): String = {
    s"drop table if exists $quote${query.dataset}$quote"
  }

  def calcResultSchema(query: Query, schema: Schema): Schema = {
    if (query.lookup.isEmpty && query.groups.isEmpty && query.select.isEmpty) {
      schema.copy()
    } else {
      ???
    }
  }

  protected def initExprMap(dataset: String, schemaMap: Map[String, AbstractSchema]): Map[String, FieldExpr] = {
    val schema = schemaMap(dataset)
    schema.fieldMap.mapValues { f =>
      FieldExpr(s"$sourceVar.$quote${f.name}$quote", s"$sourceVar.$quote${f.name}$quote")
    }
  }

  protected def parseQuery(query: Query, schemaMap: Map[String, AbstractSchema]): String = {
    val queryBuilder = new mutable.StringBuilder()

    val exprMap: Map[String, FieldExpr] = initExprMap(query.dataset, schemaMap)
    val fromStr = s"from $quote${query.dataset}$quote $sourceVar".trim
    queryBuilder.append(fromStr)
    val resultAfterAppend = parseAppend(query.append, exprMap, queryBuilder)

    val resultAfterLookup = parseLookup(query.lookup, resultAfterAppend.exprMap, queryBuilder, false)

    val resultAfterUnnest = parseUnnest(query.unnest, resultAfterLookup.exprMap, queryBuilder)
    val unnestTests = resultAfterUnnest.strs

    val resultAfterFilter = parseFilter(query.filter, resultAfterUnnest.exprMap, unnestTests, queryBuilder)

    val resultAfterGroup = parseGroupby(query.groups, resultAfterFilter.exprMap, queryBuilder)

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
        producedExprs += append.as.name -> FieldExpr(s"$sourceVar.$quote${as.name}$quote", append.definition)
      }
      producedExprs += s"$allFieldVar" -> FieldExpr(s"$allFieldVar", s"$sourceVar.$quote$allFieldVar$quote")
      val selectStr = parseProject(producedExprs.result().toMap)
      queryBuilder.insert(0, s"from ($selectStr\n")
      queryBuilder.append(s") $sourceVar")
      exprMap.foreach {
        case (field, expr) =>
          producedExprs += field -> FieldExpr(s"${expr.refExpr}", s"$sourceVar.$quote$field$quote")
      }
      val newExprMap = producedExprs.result().toMap
      ParsedResult(Seq.empty, newExprMap)
    }
  }

  private def parseLookup(lookups: Seq[LookupStatement],
                          exprMap: Map[String, FieldExpr],
                          queryBuilder: StringBuilder,
                          inGroup: Boolean): ParsedResult = {
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
        s"""left outer join $quote${lookup.dataset}$quote $lookupExpr on ${conditions.mkString(" and ")}"""
    }.mkString("\n")
    appendIfNotEmpty(queryBuilder, lookupStr)

    ParsedResult(Seq.empty, (producedExprs).result().toMap)
  }

  protected def parseFilterRelation(filter: FilterStatement, fieldExpr: String): String = {
    filter.field.dataType match {
      case DataType.Number =>
        parseNumberRelation(filter, fieldExpr)
      case DataType.Time =>
        parseTimeRelation(filter, fieldExpr)
      case DataType.Boolean => ???
      case DataType.String =>
        parseStringRelation(filter, fieldExpr)
      case DataType.Text =>
        parseTextRelation(filter, fieldExpr)
      case DataType.Bag => ???
      case DataType.Hierarchy =>
        throw new QueryParsingException("the Hierarchy type doesn't support any relations.")
      case _ => throw new QueryParsingException(s"unknown datatype: ${filter.field.dataType}")
    }
  }

  protected def parseTimeRelation(filter: FilterStatement,
                                  fieldExpr: String): String = {
    filter.relation match {
      case Relation.inRange => {
        filter.field.dataType match{
          case time: DataType.Time.type =>
            s"$fieldExpr >= '${TimeField.TimeFormatForSQL.print(new DateTime(filter.values(0).toString))}' and $fieldExpr < '${TimeField.TimeFormatForSQL.print(new DateTime(filter.values(1).toString))}'"
          case others =>
            s"$fieldExpr >= '${filter.values(0)}' and $fieldExpr < '${filter.values(1)}'"
        }
      }
      case _ => {
        filter.field.dataType match {
          case time: DataType.Time.type =>
            s"$fieldExpr ${filter.relation} '${TimeField.TimeFormatForSQL.print(new DateTime(filter.values(0).toString))}'"
          case _ =>
            s"$fieldExpr ${filter.relation} '${filter.values(0)}'"
        }
      }
    }
  }

  protected def parseStringRelation(filter: FilterStatement, fieldExpr: String): String = {
    filter.relation match {
      case Relation.matches => {
        val values = filter.values.map(_.asInstanceOf[String])
        s"""$fieldExpr="${values(0)}""""
      }
      case Relation.!= => {
        val values = filter.values.map(_.asInstanceOf[String])
        s"""$fieldExpr!="${values(0)}""""
      }
      case Relation.contains => {
        val values = filter.values.map(_.asInstanceOf[String])
        s"lower($fieldExpr) ${stringContains} '%${values(0)}%'"
      }
    }
  }

  protected def parseTextRelation(filter: FilterStatement, fieldExpr: String): String = {
    val wordsArr = ArrayBuffer[String]()
    filter.values.foreach(w => wordsArr += w.toString)
    val sb = new StringBuilder(s"${fullTextMatch(0)}($fieldExpr) ${fullTextMatch(1)} ('")
    sb.append(wordsArr.mkString("+"," +","") + s"' in boolean mode)")
    sb.toString()
  }

  protected def parseNumberRelation(filter: FilterStatement,
                                    fieldExpr: String): String = {
    filter.relation match {
      case Relation.inRange =>
        if (filter.values.size != 2) throw new QueryParsingException(s"relation: ${filter.relation} require two parameters")
        s"$fieldExpr >= ${filter.values(0)} and $fieldExpr < ${filter.values(1)}"
      case Relation.in =>
        s"$fieldExpr in ( ${filter.values.mkString(",")} )"
      case _ =>
        s"$fieldExpr ${filter.relation} ${filter.values(0)}"
    }
  }

  protected def parseAggregateFunc(aggr: AggregateStatement,
                                   fieldExpr: String): String = {
    def aggFuncExpr(aggFunc: String): String = {
      if (aggr.field.name.equals("*")) {
        s"$aggFunc($allFieldVar)"
      } else {
        s"$aggFunc($fieldExpr)"
      }
    }

    aggr.func match {
      case topK: TopK => ???
      case DistinctCount => ???
      case _ => aggFuncExpr(getAggregateStr(aggr.func))
    }
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

  //TODO: unnest
  private def parseUnnest(unnest: Seq[UnnestStatement],
                          exprMap: Map[String, FieldExpr], queryBuilder: StringBuilder): ParsedResult = {
    //return the empty result & exprMap for next step's process.
    ParsedResult((new ListBuffer[String]), exprMap)
  }

  protected def parseGroupByFunc(groupBy: ByStatement, fieldExpr: String): String = {
    groupBy.funcOpt match {
      case Some(func) =>
        func match {
          case interval: Interval => s"${timeUnitFuncMap(interval.unit)}($fieldExpr)"
          case GeoCellTenth => parseGeoCell(1, fieldExpr, groupBy.field.dataType)
          case GeoCellHundredth => parseGeoCell(2, fieldExpr, groupBy.field.dataType)
          case GeoCellThousandth => parseGeoCell(3, fieldExpr, groupBy.field.dataType)
          case bin: Bin => s"$round($fieldExpr/${bin.scale})*${bin.scale}"
          case _ => throw new QueryParsingException(s"unknown function: ${func.name}")
        }
      case None => fieldExpr
    }
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
          producedExprs += (as.name -> FieldExpr(newExpr, groupExpr))
          s"$newExpr"
        }

        val groupStr = s"group by ${groupStrs.mkString(",")}"

        appendIfNotEmpty(queryBuilder, groupStr)

        group.aggregates.foreach { aggr =>
          val fieldExpr = exprMap(aggr.field.name)
          val aggrExpr = parseAggregateFunc(aggr, fieldExpr.refExpr)
          val newExpr = s"$quote${aggr.as.name}$quote"
          producedExprs += aggr.as.name -> FieldExpr(newExpr, aggrExpr)
        }

        if (!group.lookups.isEmpty) {
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
            val expr = exprMap(orderOn.name).defExpr
            val orderStr = if (order == SortOrder.DSC) "desc" else ""
            s"${expr} $orderStr"
        }

        val orderStr =
          if (orderStrs.nonEmpty) {
            orderStrs.mkString("order by ", ",", "")
          } else {
            s""
          }

        val limitStr = s"limit ${select.limit}"
        appendIfNotEmpty(queryBuilder, orderStr)
        if (select.limit != 0) {
          appendIfNotEmpty(queryBuilder, limitStr)
        }

        if (select.fields.isEmpty || query.hasUnnest || query.hasGroup) {
          producedExprs ++= exprMap
        } else {
          select.fields.foreach {
            case AllField =>
              producedExprs += s"$allFieldVar" -> exprMap(allFieldVar)
            case field =>
              producedExprs += field.name -> exprMap(field.name)
          }
        }

        val newExprMap = producedExprs.result().toMap
        val projectStr = if (select.fields.isEmpty) {
          if (query.hasUnnest || query.hasGroup) {
            parseProject(exprMap)
          } else {
            s"select *"
          }
        } else {
          parseProject(producedExprs.result().toMap)
        }
        queryBuilder.insert(0, projectStr + "\n")
        ParsedResult(Seq.empty, newExprMap)

      case None =>
        val projectStr =
          if (query.hasUnnest || query.hasGroup) {
            parseProject(exprMap)
          } else {
            s"select *"
          }
        queryBuilder.insert(0, projectStr + "\n")
        ParsedResult(Seq.empty, exprMap)
    }
  }

  private def parseProject(exprMap: Map[String, FieldExpr]): String = {
    exprMap.map {
      case (field, expr) =>
        s"${expr.defExpr} as $quote$field$quote"
    }.mkString("select ", ",", "")
  }

  private def parseGlobalAggr(globalAggrOpt: Option[GlobalAggregateStatement],
                              exprMap: Map[String, FieldExpr],
                              queryBuilder: StringBuilder): ParsedResult = {
    globalAggrOpt match {
      case Some(globalAggr) =>
        val producedExprs = mutable.LinkedHashMap.newBuilder[String, FieldExpr]
        val aggr = globalAggr.aggregate
        val funcName = getAggregateStr(aggr.func)

        val newDefExpr = if (aggr.func == Count) {
          globalAggrVar
        } else {
          if (aggr.field.name == getAggregateStr(Count)){
            s"$quote${aggr.field.name}$quote"
          }
          else
            s"${sourceVar}.$quote${aggr.field.name}$quote"
        }
        val newRefExpr = s"$quote${aggr.as.name}$quote"
        producedExprs += aggr.as.name -> FieldExpr(newRefExpr, newDefExpr)
        val prepend =
          s"""
             |select $funcName($newDefExpr) as $quote${aggr.as.name}$quote from
             |(""".stripMargin
        val append =
          s""") ${sourceVar}""".stripMargin
        queryBuilder.insert(0, prepend)
        queryBuilder.append(append)
        ParsedResult(Seq.empty, producedExprs.result().toMap)
      case None =>
        ParsedResult(Seq.empty, exprMap)
    }
  }

  /**
    * Process POINT type of MySQL:
    * ST_ASTEXT: return POINT field as text to avoid messy code. https://dev.mysql.com/doc/refman/5.7/en/gis-format-conversion-functions.html
    * ST_X, ST_Y: get X/Y-coordinate of Point. https://dev.mysql.com/doc/refman/5.6/en/gis-point-property-functions.html
    * truncate: a number truncated to a certain number of decimal places, mainly used in groupBy. http://www.w3resource.com/mysql/mathematical-functions/mysql-truncate-function.php
    * @param scale
    * @param fieldExpr
    * @param dataType
    */
  protected def parseGeoCell(scale: Integer, fieldExpr: String, dataType: DataType.Value): String = {
    s"$geoAsText($dataType($truncate(${pointGetCoord(0)}($fieldExpr),$scale),$truncate(${pointGetCoord(1)}($fieldExpr),$scale))) "
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

object SQLGenerator extends IQLGeneratorFactory {
  override def apply(): IQLGenerator = new SQLGenerator()
}
