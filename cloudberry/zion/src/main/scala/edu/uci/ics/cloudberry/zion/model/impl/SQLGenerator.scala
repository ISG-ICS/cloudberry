package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{IQLGenerator, IQLGeneratorFactory, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema._
import play.api.libs.json.Json
import scala.collection.mutable
import scala.collection.mutable.{ListBuffer, ArrayBuffer}


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

  protected val jsonVar: String = "$"

  protected val appendVar: String = "ta"

  protected val unnestVar: String = "unnest"

  protected val lookupVar: String = "l"

  protected val groupVar: String = "*"

  protected val groupedLookupVar: String = "ll"

  protected val groupedLookupSourceVar: String = "tt"

  protected val globalAggrVar: String = "*"


  protected val quote = ""

  val round: String = "round"

  val hour: String = "hour"
  val second: String = "second"
  val minute: String = "minute"
  val day: String = "day"
  val month: String = "month"
  val year: String = "year"

  val fullTextContains: String = "like"
  val contains: String = "contains"

  val aggregateFuncMap: Map[AggregateFunc, String] = Map(
    Count -> "count",
    Max -> "max",
    Min -> "min",
    Avg -> "avg",
    Sum -> "sum"
  )

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
      case q: CreateView => parseCreate(q, schemaMap)  // TODO
      case q: AppendView => parseAppend(q, schemaMap)
      case q: UpsertRecord => parseUpsert(q, schemaMap)
      case q: DropView => parseDrop(q, schemaMap)
      case q: DeleteRecord => parseDelete(q, schemaMap)
      case _ => ???
    }
    s"$result"
  }

  def parseCreate(create: CreateView, schemaMap: Map[String, AbstractSchema]): String = {
    val (temporalSchemaMap, lookupSchemaMap) = SQLGenerator.splitSchemaMap(schemaMap)
    val sourceSchema = temporalSchemaMap(create.dataset)
    val resultSchema = calcResultSchema(create.query, sourceSchema)
    val timeFilter = s"//with filter on '${resultSchema.timeField.name}'"
    val createDataSet =
      s"""
         |drop view ${create.dataset} if exists;
         |create view ${create.dataset}(${resultSchema.typeName}) primary key ${resultSchema.primaryKey.map(_.name).mkString(",")} $timeFilter
         |""".stripMargin
    val insert =
      s"""
         |insert into view ${create.dataset} (
         |${parseQuery(create.query, Map(create.query.dataset -> sourceSchema))}
         |)
       """.stripMargin
    createDataSet + insert
  }

  protected def genDDL(schema: Schema): String = {
    //FIXME this function is wrong for nested types if it contains multiple sub-fields
    def mkNestDDL(names: List[String], typeStr: String): String = {
      names match {
        case List(e) => s"  $e : $typeStr"
        case e :: tail => s"  $e : { ${mkNestDDL(tail, typeStr)} }"
      }
    }

    val fields = schema.fieldMap.values.filter(f => f.dataType != DataType.Hierarchy && f != AllField).map {
      f => mkNestDDL(f.name.split("\\.").toList, fieldType2ADMType(f) + (if (f.isOptional) "?" else ""))
    }
    s"""
       |create type ${schema.typeName} if not exists as open {
       |${fields.mkString(",\n")}
       |}
      """.stripMargin
  }

  protected def fieldType2ADMType(field: Field): String = {
    field.dataType match {
      case DataType.Number => "double"
      case DataType.Time => "datetime"
      case DataType.Point => "point"
      case DataType.Boolean => "tinyint"
      case DataType.String => "varchar"
      case DataType.Text => "text"
      case DataType.Json => "json"
      case DataType.Bag => s"{{${fieldType2ADMType(Field("", field.asInstanceOf[BagField].innerType))}}}"
      case DataType.Hierarchy => ???
      case DataType.Record => ???
    }
  }

  def parseAppend(append: AppendView, schemaMap: Map[String, AbstractSchema]): String = {
    s"""
       |insert into ${append.dataset} (
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
    queryBuilder.toString()
  }

  protected def parseDrop(query: DropView, schemaMap: Map[String, AbstractSchema]): String = {
    s"drop view ${query.dataset} if exists"
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
      f.dataType match {
        case _ => {
          val quoted = f.name.split('.').map(name => s"$quote$name$quote").mkString(".")
          FieldExpr(s"$sourceVar.$quoted", s"$sourceVar.$quoted")
        }
      }
    }
  }

  protected def parseQuery(query: Query, schemaMap: Map[String, AbstractSchema]): String = {
    val queryBuilder = new mutable.StringBuilder()

    val exprMap: Map[String, FieldExpr] = initExprMap(query.dataset, schemaMap)
    val fromStr = s"from ${query.dataset} $sourceVar".trim
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
        println("parseGroupby: parseAppend(unnest): " + append.as.name + "->" + FieldExpr(s"$appendVar.${as.name}", append.definition).toString())
        producedExprs += append.as.name -> FieldExpr(s"$appendVar.${as.name}", append.definition)
      }
      exprMap.foreach {
        case (field, expr) =>
          println("parseGroupby: parseAppend(unnest2): " + field + "->" + FieldExpr(s"$appendVar.$field", s"${expr.refExpr}").toString())
          producedExprs += field -> FieldExpr(s"$appendVar.$field", s"${expr.refExpr}")
      }
      val newExprMap = producedExprs.result().toMap
      val selectStr = parseProject(newExprMap)
      queryBuilder.insert(0, s"from ($selectStr\n")
      queryBuilder.append(s") $appendVar")
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
            println("parseLookup: " + as.name + "->" + FieldExpr(expr, expr).toString())
            producedExprs += (as.name -> FieldExpr(expr, expr))
        }
        s"""left outer join ${lookup.dataset} $lookupExpr on ${conditions.mkString(" and ")}"""
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
      case DataType.Json =>
        parseJsonRelation(filter, fieldExpr)
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
        s"$fieldExpr >= '${filter.values(0)}' and $fieldExpr < '${filter.values(1)}'"
      }
      case _ => {
        s"$fieldExpr ${filter.relation} '${filter.values(0)}'"
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
      case Relation.contains => ???

    }
  }

  protected def parseTextRelation(filter: FilterStatement, fieldExpr: String): String = {
    val wordsArr = ArrayBuffer[String]()
    filter.values.foreach(w => wordsArr += "%" + w + "%")
    val sb = new StringBuilder
    for (i <- 0 until (wordsArr.length - 1)){
      sb.append(s"lower($fieldExpr) ${fullTextContains} '${wordsArr(i)}' and ")
    }
    sb.append(s"lower($fieldExpr) ${fullTextContains} '${wordsArr(wordsArr.length - 1)}'")
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

  protected def parseJsonRelation(filter: FilterStatement, fieldExpr: String): String = {
    filter.relation match  {
      case Relation.inRange =>
        if (filter.values.size != 2) throw new QueryParsingException(s"relation: ${filter.relation} require two parameters")
        s"$fieldExpr >= ${filter.values(0)} and $fieldExpr < ${filter.values(1)}"
      case Relation.in =>
//        if ()
//        s"""$fieldExpr in ( "${filter.values.mkString("\",\"")}" )"""  //TODO: add a if else for string and int
        s"""$fieldExpr in ( ${filter.values.mkString(",")} )"""
      case Relation.contains =>
        val sb = new StringBuilder
        val wordsArr = ArrayBuffer[String]()
        filter.values.foreach(w => wordsArr += "%" + w + "%")
        for (i <- 0 until (wordsArr.length - 1)){
          sb.append(s"lower($fieldExpr) ${fullTextContains} '${wordsArr(i)}' and ")
        }
        sb.append(s"lower($fieldExpr) ${fullTextContains} '${wordsArr(wordsArr.length - 1)}'")
        sb.toString()
      case Relation.matches => {
        val values = filter.values.map(_.asInstanceOf[String])
        s"""$fieldExpr="${values(0)}""""
      }
      case _ => {
        s"$fieldExpr ${filter.relation} ${filter.values(0)}"
      }
    }
  }

  protected def parseAggregateFunc(aggr: AggregateStatement,
                                   fieldExpr: String): String = {
    def aggFuncExpr(aggFunc: String): String = {
      if (aggr.field.name.equals("*")) {
        s"$aggFunc($groupVar)"
      } else {
        s"$aggFunc($fieldExpr)"
      }
    }

    aggr.func match {
      case topK: TopK => ???
      case DistinctCount => ???
      case _ =>
        aggFuncExpr(getAggregateStr(aggr.func))
    }
  }

  private def parseFilter(filters: Seq[FilterStatement], exprMap: Map[String, FieldExpr], unnestTestStrs: Seq[String], queryBuilder: StringBuilder): ParsedResult = {
    if (filters.isEmpty && unnestTestStrs.isEmpty) {
      ParsedResult(Seq.empty, exprMap)
    } else {
      val filterStrs = filters.map { filter =>
        if (filter.field.name.contains(".")) {
          parseFilterRelation(filter, sourceVar + '.' + filter.field.name.replaceFirst("\\.","->\"\\$.") + '"')
        } else {
          parseFilterRelation(filter, exprMap(filter.field.name).refExpr)
        }
      }
//      println("paprseFilter: exprMap: " + exprMap.toString())
      val filterStr = (unnestTestStrs ++ filterStrs).mkString("where ", " and ", "")
      appendIfNotEmpty(queryBuilder, filterStr)
      ParsedResult(Seq.empty, exprMap)
    }
  }

  protected def parseGeoCell(scale: Double, fieldExpr: String, dataType: DataType.Value): String = ???

  private def parseUnnest(unnest: Seq[UnnestStatement],
                          exprMap: Map[String, FieldExpr], queryBuilder: StringBuilder): ParsedResult = {
    val producedExprs = mutable.LinkedHashMap.newBuilder[String, FieldExpr]
    val unnestTestStrs = new ListBuffer[String]
    val unnestStr = unnest.zipWithIndex.map {
      case (unnest, id) =>
        val expr = exprMap(unnest.field.name)
        val newExpr = s"${quote}hashtag$quote"
        println("parseGroupby: parseGroupby(unnest): " + unnest.as.name + "->" + FieldExpr(newExpr, newExpr).toString())
        producedExprs += (unnest.as.name -> FieldExpr(newExpr, newExpr))
        if (unnest.field.isOptional) {
          unnestTestStrs += s"${expr.refExpr} is not null"
        }
        s"" // literal view is not allowed in MySQL
    }.mkString("\n")
    appendIfNotEmpty(queryBuilder, unnestStr)

    ParsedResult(unnestTestStrs.toSeq, (producedExprs ++= exprMap).result().toMap)
  }

  protected def parseGroupByFunc(groupBy: ByStatement, fieldExpr: String): String = {
    groupBy.funcOpt match {
      case Some(func) =>
        func match {
          case interval: Interval =>
            s"${interval.unit}($fieldExpr)"
//            s"parseGroupByFunc: is interval, name: " + interval.toString() + " unit: " + interval.unit + " scale: " + interval.scale
          case bin: Bin => ???
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
          val fieldExpr = if (by.field.name.contains(".")) {
            println("by is: " + by.toString())
            FieldExpr(by.field.name.split("\\.", 2).head + "->\"$." + by.field.name.split("\\.", 2)(1) + '"', by.field.name)
//            exprMap(by.field.name.split("\\.", 2).head)  //TODO: test if this change would inform anything
          } else {
            exprMap(by.field.name)
          }
          val as = by.as.getOrElse(by.field)
          println()
          val groupExpr = parseGroupByFunc(by, fieldExpr.refExpr)
          val newExpr = s"$quote${as.name}$quote"
          println("parseGroupby: parseGroupby: " + as.name + "->" + FieldExpr(newExpr, groupExpr).toString())
          producedExprs += (as.name -> FieldExpr(newExpr, groupExpr))

          if (newExpr == s"${quote}hashtag${quote}" || newExpr == s"${quote}tag${quote}" || newExpr == s"${quote}hashtags${quote}"){
            s"${quote}hashtag${quote}"
          }
          else if (by.field.dataType == DataType.Json) {
            println("ParseGroupby: by.field.name: " + by.field.name)
            println("ParseGroupby: as.name: " + as.name)
            println("ParseGroupby: newExpr: " + newExpr)
            s"$newExpr"
          }
          else{
            s"$newExpr"
          }
        }

        val groupStr = s"group by ${groupStrs.mkString(",")}"

        appendIfNotEmpty(queryBuilder, groupStr)

        group.aggregates.foreach { aggr =>
          val fieldExpr = exprMap(aggr.field.name)
          println("parseGroupby: aggr: " + aggr.toString())
          println("parseGroupby: fieldExpr: " + fieldExpr.toString())
          val aggrExpr = parseAggregateFunc(aggr, fieldExpr.refExpr)
          println("parseGroupby: aggrExpr: " + aggrExpr)
          val newExpr = s"$quote${aggr.as.name}$quote"
          println("parseGroupby: parseGroupby(group): " + aggr.as.name + "->" + FieldExpr(newExpr, aggrExpr).toString())
          producedExprs += aggr.as.name -> FieldExpr(newExpr, aggrExpr)
        }
        println("parseGroupby: producedExprs : " + producedExprs.toString() )

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
          ParsedResult(Seq.empty, producedExprs.result().toMap)   // TODO: should ++ exprMap ?
        }
      case None => ParsedResult(Seq(""), exprMap)
    }
  }

  private def parseSelect(selectOpt: Option[SelectStatement],
                          exprMap: Map[String, FieldExpr], query: Query,
                          queryBuilder: StringBuilder): ParsedResult = {

    selectOpt match {
      case Some(select) =>
//        println("parseSelect: selectOpt: " + selectOpt.toString())
        val producedExprs = mutable.LinkedHashMap.newBuilder[String, FieldExpr]

        val orderStrs = select.orderOn.zip(select.order).map {
          case (orderOn, order) =>
            val expr = exprMap(orderOn.name).refExpr
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

        if (select.fields.isEmpty || query.hasUnnest || query.hasGroup) {  // TODO: just appended
          producedExprs ++= exprMap
        } else {
          select.fields.foreach {
            case AllField =>
              producedExprs ++= exprMap
            case field =>
              println("SQLGemerator: parseSelect: exprMap: " + exprMap.toString())
              println("SQLGemerator: parseSelect: field:" + field.toString())
              if (field.name.contains(".")) {
                println("parseSelect: " + field.name + "->" + FieldExpr(field.name.replace(".", "_"), sourceVar + "." + field.name).toString())
                producedExprs += field.name -> FieldExpr(field.name.replace(".", "_"), sourceVar + "." + field.name)
              } else {
                println("parseSelect: " + field.name + "->" + exprMap(field.name).toString())
                producedExprs += field.name -> exprMap(field.name)  //TODO: += as.name -> exprMap(as.name)
              }
          }
        }

        val newExprMap = producedExprs.result().toMap
        val projectStr = if (select.fields.isEmpty) {
          if (query.hasUnnest || query.hasGroup) {  //TODO
            parseProject(exprMap)
          } else {
            s"select *"
          }
        } else {
          parseProject(newExprMap)
        }
        queryBuilder.insert(0, projectStr + "\n")
        ParsedResult(Seq.empty, newExprMap)

      case None =>
        val projectStr =
          if (query.hasUnnest || query.hasGroup) {  //TODO
            parseProject(exprMap)
          } else {
            s"select *"
          }
        queryBuilder.insert(0, projectStr + "\n")
        ParsedResult(Seq.empty, exprMap)
    }
  }

  private def parseProject(exprMap: Map[String, FieldExpr]): String = {
    exprMap.filter {
      case (field, expr) => field != "*" && expr.refExpr != sourceVar
    }.map {
      case (field, expr) =>
        if (s"${expr.defExpr}" == s"$quote$second$quote" || s"${expr.defExpr}" == s"$quote$minute$quote" || s"${expr.defExpr}" == s"$quote$hour$quote" || s"${expr.defExpr}" == s"$quote$day$quote" || s"${expr.defExpr}" == s"$quote$month$quote" || s"${expr.defExpr}" == s"$quote$year$quote"){
          s"${expr.defExpr}($sourceVar.${quote}create_at${quote}) as ${expr.defExpr}"
        }
        else if (expr.defExpr.count(_ == '.') > 1) {
          val defExpr = expr.defExpr.split("\\.", 3)(0) + "." + expr.defExpr.split("\\.", 3)(1) + "->\"$." + expr.defExpr.split("\\.", 3)(2) + "\""
          s"${defExpr} as ${expr.refExpr}"
        }
        else {
          s"${expr.defExpr} as $quote$field$quote"
        }
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
        println("parseGlobalAggr: " + aggr.as.name + "->" + FieldExpr(newRefExpr, newDefExpr).toString())
        producedExprs += aggr.as.name -> FieldExpr(newRefExpr, newDefExpr)
        val prepend =
          s"""
             |select $funcName($newDefExpr) as $quote${aggr.as.name}$quote from
             |(""".stripMargin
        val append =
          s""") ${appendVar}""".stripMargin
        queryBuilder.insert(0, prepend)
        queryBuilder.append(append)
        ParsedResult(Seq.empty, producedExprs.result().toMap)
      case None =>
        ParsedResult(Seq.empty, exprMap)
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

object SQLGenerator extends IQLGeneratorFactory {
  override def apply(): IQLGenerator = new SQLGenerator()

  def splitSchemaMap(schemaMap: Map[String, AbstractSchema]): (Map[String, Schema], Map[String, LookupSchema]) = {
    val temporalSchemaMap = scala.collection.mutable.Map[String, Schema]()
    val lookupSchemaMap = scala.collection.mutable.Map[String, LookupSchema]()

    schemaMap.filter{ case(name, schema) =>
      schema.isInstanceOf[Schema]
    }.foreach{ case(name, schema) =>
      temporalSchemaMap.put(name, schema.asInstanceOf[Schema])
    }

    schemaMap.filter{ case(name, schema) =>
      schema.isInstanceOf[LookupSchema]
    }.foreach{ case(name, schema) =>
      lookupSchemaMap.put(name, schema.asInstanceOf[LookupSchema])
    }

    (temporalSchemaMap.toMap, lookupSchemaMap.toMap)
  }
}
