package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{IQLGenerator, IQLGeneratorFactory, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema._
import play.api.libs.json.Json
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.ArrayBuffer


class SparkSQLGenerator extends IQLGenerator {

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

  //  protected val typeImpl: SparkSqlImpl

  protected val sourceVar: String = "t"

  protected val appendVar: String = "ta"

  protected val unnestVar: String = "unnest"

  protected val lookupVar: String = "l"

  protected val groupVar: String = "*"

  protected val groupedLookupVar: String = "ll"

  protected val groupedLookupSourceVar: String = "tt"

  protected val globalAggrVar: String = "*"

  protected val outerSelectVar: String = "s"

  protected val quote = "`"

  protected val suffix: String = ";"

  val datetime: String = ""
  val round: String = "round"

  val dayTimeDuration: String = "day_time_duration"
  val yearMonthDuration: String = "year_month_duration"
  val getIntervalStartDatetime: String = "get_interval_start_datetime"
  val intervalBin: String = "interval_bin"
  val hour: String = "hour"

  val spatialIntersect: String = "spatial_intersect"
  val createRectangle: String = "create_rectangle"
  val createPoint: String = "create_point"
  val spatialCell: String = "spatial_cell"
  val getPoints: String = "get_points"

  val similarityJaccard: String = "similarity_jaccard"
  val fullTextContains: String = "like"
  val contains: String = "contains"
  val wordTokens: String = "word_tokens"

  val aggregateFuncMap: Map[AggregateFunc, String] = Map(
    Count -> "count",
    Max -> "max",
    Min -> "min",
    Avg -> "avg",
    Sum -> "sum"
  )

  def getAggregateStr(aggregate: AggregateFunc): String = {
    aggregateFuncMap.get(aggregate) match {
      case Some(impl) =>
        impl
      case None =>
        throw new QueryParsingException(s"No implementation is provided for aggregate function ${aggregate.name}")
    }
  }


  def generate(query: IQuery, schemaMap: Map[String, Schema]): String = {
    val result = query match {
      case q: Query => parseQuery(q, schemaMap)
      case q: CreateView => parseCreate(q, schemaMap)
      //      case q: AppendView => parseAppend(q, schemaMap)
      //      case q: UpsertRecord => parseUpsert(q, schemaMap)
      //      case q: DropView => parseDrop(q, schemaMap)
      //      case q: DeleteRecord => parseDelete(q, schemaMap)
      case _ => ???
    }
    s"$result$suffix"
  }


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
      case DataType.Boolean => "boolean"
      case DataType.String => "string"
      case DataType.Text => "string"
      case DataType.Bag => s"{{${fieldType2ADMType(Field("", field.asInstanceOf[BagField].innerType))}}}"
      case DataType.Hierarchy => ??? // should be skipped
      case DataType.Record => ???
    }
  }

  //  def parseAppend(append: AppendView, schemaMap: Map[String, Schema]): String = {
  //    s"""
  //       |upsert into ${append.dataset} (
  //       |${parseQuery(append.query, schemaMap)}
  //       |)""".stripMargin
  //  }
  //
  //  def parseUpsert(q: UpsertRecord, schemaMap: Map[String, Schema]): String = {
  //    s"""
  //       |upsert into ${q.dataset} (
  //       |${Json.toJson(q.records)}
  //       |)""".stripMargin
  //  }
  //
  //  protected def parseDelete(delete: DeleteRecord, schemaMap: Map[String, Schema]): String = {
  //    if (delete.filters.isEmpty) {
  //      throw new QueryParsingException("Filter condition is required for DeleteRecord query.")
  //    }
  //    val exprMap: Map[String, FieldExpr] = initExprMap(delete.dataset, schemaMap)
  //    val queryBuilder = new StringBuilder()
  //    queryBuilder.append(s"delete from ${delete.dataset} $sourceVar")
  //    parseFilter(delete.filters, exprMap, Seq.empty, queryBuilder)
  //    return queryBuilder.toString()
  //  }
  //
  //  protected def parseDrop(query: DropView, schemaMap: Map[String, Schema]): String = {
  //    s"drop dataset ${query.dataset} if exists"
  //  }


  def calcResultSchema(query: Query, schema: Schema): Schema = {
    if (query.lookup.isEmpty && query.groups.isEmpty && query.select.isEmpty) {
      schema.copy()
    } else {
      ???
    }
  }

  protected def initExprMap(dataset: String, schemaMap: Map[String, Schema]): Map[String, FieldExpr] = {
    val schema = schemaMap(dataset)
    schema.fieldMap.mapValues { f =>
      f.dataType match {
        case DataType.Record => FieldExpr(sourceVar, sourceVar)
        case DataType.Hierarchy => FieldExpr(sourceVar, sourceVar) // TODO rethink this type: a type or just a relation between types?
        case _ => {
          //Add the quote to wrap the name in order to not touch the SQL reserved keyword
          val quoted = f.name.split('.').map(name => s"$quote$name$quote").mkString(".")
          FieldExpr(s"$sourceVar.$quoted", s"$sourceVar.$quoted")
        }
      }
    }
  }

  def parseQuery(query: Query, schemaMap: Map[String, Schema]): String = {
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
        producedExprs += append.as.name -> FieldExpr(s"$appendVar.${as.name}", append.definition)
      }
      exprMap.foreach {
        case (field, expr) =>
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





  protected def parseFilterRelation(filter: FilterStatement, fieldExpr: String): String = {
    filter.field.dataType match {
      case DataType.Number =>
        parseNumberRelation(filter, fieldExpr)
      case DataType.Time =>
        parseTimeRelation(filter, fieldExpr)
      //      case DataType.Point =>
      //        parsePointRelation(filter, fieldExpr)
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


  protected def parseGeoCell(scale: Double, fieldExpr: String, dataType: DataType.Value): String = {
    val origin = s"${createPoint}(0.0,0.0)"
    s"${getPoints}(${spatialCell}(${fieldExpr}, $origin, ${1 / scale}, ${1 / scale}))[0]"
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

  private def parseUnnest(unnest: Seq[UnnestStatement],
                          exprMap: Map[String, FieldExpr], queryBuilder: StringBuilder): ParsedResult = {
    val producedExprs = mutable.LinkedHashMap.newBuilder[String, FieldExpr]
    val unnestTestStrs = new ListBuffer[String]
    val unnestStr = unnest.zipWithIndex.map {
      case (unnest, id) =>
        val expr = exprMap(unnest.field.name)
        //        val newExpr = s"${quote}hash$id$quote"
        val newExpr = s"${quote}hash$quote"
        producedExprs += (unnest.as.name -> FieldExpr(newExpr, newExpr))
        if (unnest.field.isOptional) {
          unnestTestStrs += s"${expr.refExpr} is not null"
        }
        s"lateral view explode(${expr.refExpr}) tab as $newExpr"
    }.mkString("\n")
    appendIfNotEmpty(queryBuilder, unnestStr)

    ParsedResult(unnestTestStrs.toSeq, (producedExprs ++= exprMap).result().toMap)
  }

  protected def parseGroupByFunc(groupBy: ByStatement, fieldExpr: String): String = {
    groupBy.funcOpt match {
      case Some(func) =>
        func match {
          case bin: Bin => s"${round}($fieldExpr/${bin.scale})*${bin.scale}"
          //          case interval: Interval =>
          //            val duration = parseIntervalDuration(interval)
          //            s"${typeImpl.getIntervalStartDatetime}(${typeImpl.intervalBin}($fieldExpr, '1990-01-01T00:00:00.000Z', $duration))"
          case interval: Interval => s"$fieldExpr"
          //get_interval_start_datetime(interval_bin(t.`create_at`, '1990-01-01T00:00:00.000Z',  day_time_duration("PT1H") ))
          case level: Level =>
            //TODO remove this data type
            val hierarchyField = groupBy.field.asInstanceOf[HierarchyField]
            val field = hierarchyField.levels.find(_._1 == level.levelTag).get
            s"$fieldExpr.${field._2}"
          case GeoCellTenth => parseGeoCell(10, fieldExpr, groupBy.field.dataType)
          case GeoCellHundredth => parseGeoCell(100, fieldExpr, groupBy.field.dataType)
          case GeoCellThousandth => parseGeoCell(1000, fieldExpr, groupBy.field.dataType)
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
          producedExprs += (as.name -> FieldExpr(newExpr, newExpr))
          if (newExpr != s"`hash`"){
            s"$newExpr($groupExpr)"
          }
          else{
            s"$groupExpr"
          }
        }
        val groupStr = s"group by ${groupStrs.mkString(",")}"
        //        println(groupStrs)
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
    println("exprMap is:", exprMap)
    selectOpt match {
      case Some(select) =>
        val producedExprs = mutable.LinkedHashMap.newBuilder[String, FieldExpr]
        println("producedExprs is:", producedExprs)

        val orderStrs = select.orderOn.zip(select.order).map {
          case (orderOn, order) =>
            val expr = exprMap(orderOn.name).refExpr
            val orderStr = if (order == SortOrder.DSC) "desc" else ""
            s"${expr} $orderStr"
          //          "order by `count` desc": expr = `count`(orderOn), orderStr = desc
        }
        println("orderStrs is:", orderStrs)

        val orderStr =
          if (!orderStrs.isEmpty) {
            orderStrs.mkString("order by ", ",", "")
          } else {
            ""
          }
        println("orderStr is:", orderStr)

        val limitStr = s"limit ${select.limit}"
        appendIfNotEmpty(queryBuilder, orderStr)
        if (select.limit != 0) {
          appendIfNotEmpty(queryBuilder, limitStr)
        }


        if (select.fields.isEmpty) {
          producedExprs ++= exprMap
          println("producedExprs after ++:", producedExprs)
        } else {
          println("exprMap is:", exprMap)
          select.fields.foreach {
            case AllField =>
              producedExprs ++= exprMap
            case field =>
              if (field.name == "create_at"){
                //                producedExprs +=
              }
              else {
                producedExprs += field.name -> exprMap(field.name)
              }
          }
        }
        val newExprMap = producedExprs.result().toMap
        println("newExprMap:", newExprMap)
        //check if fields is empty
        val projectStr = if (select.fields.isEmpty) {
          //          if (query.hasUnnest || query.hasGroup) {
          //            parseProject(exprMap)
          //          } else {
          //            s"select *"
          //          }
          s"select *"
        } else {
          //          parseProject(newExprMap)
          parseProject(exprMap)
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
    exprMap.filter {
      case (field, expr) => field != "*" && expr.refExpr != sourceVar
    }.map {
      case (field, expr) =>
        if (s"${expr.defExpr}" == "`hour`" || s"${expr.defExpr}" == "`day`" || s"${expr.defExpr}" == "`month`"){
          //          s"${expr.defExpr}(t.`create_at`) as $quote$field$quote"
          s"${expr.defExpr}(t.`create_at`)"
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
          s"${sourceVar}.$quote${aggr.field.name}$quote"
        }
        val newRefExpr = s"$quote${aggr.as.name}$quote"

        producedExprs += aggr.as.name -> FieldExpr(newRefExpr, newDefExpr)
        val prepend =
          s"""
             |select $funcName($newDefExpr) as $quote${aggr.as.name}$quote from
             |(""".stripMargin
        val append =
          s""")""".stripMargin
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
        s"$fieldExpr in ( ${filter.values.mkString(",")} )"
      case _ =>
        s"$fieldExpr ${filter.relation} ${filter.values(0)}"
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

//object SparkSQLGenerator extends IQLGeneratorFactory {
//  override def apply(): IQLGenerator = new SparkSQLGenerator()
//}
