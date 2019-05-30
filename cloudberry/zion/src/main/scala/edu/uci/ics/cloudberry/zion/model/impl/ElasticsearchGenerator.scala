package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{IQLGenerator, IQLGeneratorFactory, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema._

import play.api.libs.json._

import scala.collection.mutable

/**
  * Defines constant strings for query languages supported by Elasticsearch
  */
trait ElasticImpl {

  val aggregateFuncMap: Map[AggregateFunc, String]

  def getAggregateStr(aggregate: AggregateFunc): String = {
    aggregateFuncMap.get(aggregate) match {
      case Some(impl) => impl
      case None => throw new QueryParsingException(s"No implementation is provided for aggregate function ${aggregate.name}")
    }
  }
}

object ElasticImpl extends ElasticImpl {
  val aggregateFuncMap: Map[AggregateFunc, String] = Map(
    Count -> "count",
    Max -> "max",
    Min -> "min",
    Avg -> "avg",
    Sum -> "sum"
  )
}


class ElasticsearchGenerator extends IQLGenerator {

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

  protected val allFieldVar: String = "*"

  protected var joinTermsFilter = Seq[Int]() // Global variable: JOIN field

  protected val typeImpl: ElasticImpl = ElasticImpl

  def generate(query: IQuery, schemaMap: Map[String, AbstractSchema]): String = {
    val temporalSchemaMap = GeneratorUtil.splitSchemaMap(schemaMap)._1
    val result = query match {
      case q: Query => parseQuery(q, temporalSchemaMap)
      case q: CreateView => parseCreate(q, temporalSchemaMap)
      case q: AppendView => parseAppend(q, temporalSchemaMap)
      case q: UpsertRecord => parseUpsert(q, schemaMap)
      case _ => ???
    }
    result
  }

  def calcResultSchema(query: Query, schema: Schema): Schema = {
    if (query.lookup.isEmpty && query.groups.isEmpty && query.select.isEmpty) {
      schema.copySchema
    } else {
      ???
    }
  }

  private def parseQuery(query: Query, schemaMap: Map[String, Schema]): String = {
    var queryBuilder = Json.obj()

    val exprMap: Map[String, FieldExpr] = initExprMap(query.dataset, schemaMap)

    queryBuilder += ("method" -> JsString("search"))
    queryBuilder += ("dataset" -> JsString(query.dataset))

    val exprMapAfterUnnest = parseUnnest(query.unnest, exprMap)

    val (resultAfterFilter, queryAfterFilter) = parseFilter(query.filter, exprMapAfterUnnest, queryBuilder)

    val (resultAfterGroup, queryAfterGroup) = parseGroupby(query.groups, query.unnest, query.select, resultAfterFilter.exprMap, queryAfterFilter)

    val (resultAfterSelect, queryAfterSelect) = parseSelect(query.select, query.groups, resultAfterGroup.exprMap, queryAfterGroup)

    val queryAfterGlobalAggr = parseGlobalAggr(query.globalAggr, resultAfterSelect.exprMap, queryAfterSelect)

    queryAfterGlobalAggr.toString()

  }

  private def parseCreate(create: CreateView, schemaMap: Map[String, Schema]): String = {
    var source = Json.obj()
    val dataset = create.dataset
    var reindexStatement = Json.obj()

    val dropStatement = Json.parse(s"""{ "method": "drop", "dataset": "$dataset" }""")
    val createStatement = Json.parse(s"""{ "method": "create", "dataset": "$dataset"}""")
    val selectStatement = Json.parse(parseQuery(create.query, schemaMap)).as[JsObject]
    val dest = Json.parse(s""" { "index": "$dataset" } """)

    source += ("index" -> JsString(create.query.dataset))
    source += ("query" -> (selectStatement \ "query").get)

    reindexStatement += ("method" -> JsString("reindex"))
    reindexStatement += ("source" -> source)
    reindexStatement += ("dest" -> dest)

    val resQueryArray = Json.arr(dropStatement, createStatement, reindexStatement)
    resQueryArray.toString()
  }

  private def parseAppend(append: AppendView, schemaMap: Map[String, Schema]): String = {
    val selectStatement = Json.parse(parseQuery(append.query, schemaMap)).as[JsObject]
    val dest = Json.parse(s""" { "index": "${append.dataset}" } """)
    var reindexStatement = Json.obj()
    var source = Json.obj()

    source += ("index" -> JsString(append.query.dataset))
    source += ("query" -> (selectStatement \ "query").get)

    reindexStatement += ("method" -> JsString("reindex"))
    reindexStatement += ("source" -> source)
    reindexStatement += ("dest" -> dest)

    reindexStatement.toString()
  }

  private def parseUpsert(q: UpsertRecord, schemaMap: Map[String, AbstractSchema]): String = {
    var queryBuilder = Json.obj()
    var recordBuilder = Json.arr()
    val schema = schemaMap(q.dataset)
    val primaryKeyList = schema.getPrimaryKey

    for (record <- q.records.as[List[JsValue]]) {
      val idBuilder = new StringBuilder()
      var docBuilder = Json.obj()
      // Assume primary key is not a subfield of JSON data
      for (field <- primaryKeyList) {
        val id = (record \ (field.name)).get.toString()
        idBuilder.append(id)
      }
      recordBuilder = recordBuilder :+ Json.obj(("update" -> Json.obj("_id" -> Json.parse(idBuilder.toString))))
      docBuilder += ("doc" -> record)
      docBuilder += ("doc_as_upsert" -> JsBoolean(true))
      recordBuilder = recordBuilder :+ docBuilder
    }

    queryBuilder += ("method" -> JsString("upsert"))
    queryBuilder += ("dataset" -> JsString(q.dataset))
    queryBuilder += ("records" -> recordBuilder)
    queryBuilder.toString()
  }

  private def parseUnnest(unnest: Seq[UnnestStatement],
                          exprMap: Map[String, FieldExpr]): Map[String, FieldExpr] = {
    val producedExprs = mutable.LinkedHashMap.newBuilder[String, FieldExpr]

    unnest.foreach {
      u =>
        val expr = exprMap(u.field.name)
        producedExprs += (u.as.name -> expr)
    }

    (producedExprs ++= exprMap).result().toMap
  }

  private def parseFilter(filters: Seq[FilterStatement], exprMap: Map[String, FieldExpr], queryBuilder: JsObject): (ParsedResult, JsObject) = {
    var shallowQuery = queryBuilder

    if (filters.nonEmpty) {
      val filterStrs = filters.map { filter =>
        parseFilterRelation(filter, exprMap(filter.field.name).refExpr)
      }
      val filterStr = filterStrs.mkString("""{"must": [""", ",", "]}")

      shallowQuery += ("query" -> Json.obj("bool" -> Json.parse(filterStr)))

      return (ParsedResult(Seq.empty, exprMap), shallowQuery)
    }
    (ParsedResult(Seq.empty, exprMap), shallowQuery)
  }

  private def parseGroupby(groupOpt: Option[GroupStatement],
                           unnest: Seq[UnnestStatement],
                           selectOpt: Option[SelectStatement],
                           exprMap: Map[String, FieldExpr],
                           queryAfterAppend: JsObject): (ParsedResult, JsObject) = {
    var shallowQueryAfterAppend = queryAfterAppend
    groupOpt match {
      case Some(group) =>
        shallowQueryAfterAppend += ("size" -> JsNumber(0))
        val groupStr = new mutable.StringBuilder()
        val isLookUp = group.lookups.isEmpty
        var groupAsArray = Json.arr()
        for (i <- group.bys.indices) {
          val by = group.bys(i)
          val fieldExpr = exprMap(by.field.name)
          val as = by.as.getOrElse(by.field).name
          groupAsArray = groupAsArray.append(JsString(as))

          if (i == 0)
            groupStr.append(s"""{"$as": {""")
          else
            groupStr.append(s""","aggs": {"$as": {""")
          parseGroupByFunc(by, fieldExpr.refExpr, groupStr, isLookUp, selectOpt)
        }
        groupStr.append("}" * group.bys.length * 2)
        shallowQueryAfterAppend += ("aggs" -> Json.parse(groupStr.toString))

        if (group.lookups.nonEmpty) {
          var queryArray = Json.arr()
          val selectDataset = (shallowQueryAfterAppend \ "dataset").get.as[JsString]
          shallowQueryAfterAppend -= "dataset"
          shallowQueryAfterAppend -= "method"
          queryArray = queryArray :+ Json.obj("index" -> selectDataset)
          queryArray = queryArray :+ shallowQueryAfterAppend
          val body = group.lookups.head
          val joinQuery = Json.parse(s"""{"_source": "${body.selectValues.head.name}", "size": 2147483647, "sort": {"${body.lookupKeys.head.name}": { "order": "asc" }}, "query": {"bool": {"must": {"terms": { "${body.lookupKeys.head.name}" : ${joinTermsFilter.mkString("[", ",", "]")} } } } } }""").as[JsObject]
          queryArray = queryArray :+ Json.obj("index" -> JsString(body.dataset.toLowerCase())) // The name of dataset in Elasticsearch must be in lowercase.
          queryArray = queryArray :+ joinQuery

          var multiSearchQuery = Json.obj("method" -> JsString("msearch"))
          multiSearchQuery += ("queries" -> queryArray)
          multiSearchQuery += ("groupAsList" -> groupAsArray)
          multiSearchQuery += ("joinSelectField" -> JsString(body.selectValues.head.name))
          multiSearchQuery += ("joinTermsFilter" -> Json.toJson(joinTermsFilter))

          return (ParsedResult(Seq.empty, exprMap), multiSearchQuery)
        }
        shallowQueryAfterAppend += ("groupAsList" -> groupAsArray)
        (ParsedResult(Seq.empty, exprMap), shallowQueryAfterAppend)

      case None =>
        (ParsedResult(Seq.empty, exprMap), shallowQueryAfterAppend)
    }
  }

  private def parseSelect(selectOpt: Option[SelectStatement],
                          groupOpt: Option[GroupStatement],
                          exprMap: Map[String, FieldExpr],
                          queryAfterGroup: JsObject): (ParsedResult, JsObject) = {
    var shallowQueryAfterGroup = queryAfterGroup

    if (groupOpt.isEmpty && selectOpt.nonEmpty) {
      val select = selectOpt.get
      val orderStrs = select.orderOn.zip(select.order).map {
        case (orderOn, order) =>
          val expr = orderOn.name
          val orderStr = if (order == SortOrder.DSC) "desc" else "asc"
          s"""{"$expr": {"order": "$orderStr"}}"""
      }
      val orderStr =
        if (orderStrs.nonEmpty) {
          orderStrs.mkString("[", ",", "]")
        } else {
          ""
        }
      shallowQueryAfterGroup += ("size" -> JsNumber(select.limit))
      shallowQueryAfterGroup += ("from" -> JsNumber(select.offset))
      val source = select.fields.map(f => JsString(f.name))

      if (orderStr.nonEmpty)
        shallowQueryAfterGroup += ("sort" -> Json.parse(orderStr))
      if (source.nonEmpty)
        shallowQueryAfterGroup += ("_source" -> JsArray(source))
    }
    (ParsedResult(Seq.empty, exprMap), shallowQueryAfterGroup)
  }

  private def parseGlobalAggr(globalAggrOpt: Option[GlobalAggregateStatement],
                              exprMap: Map[String, FieldExpr], queryAfterSelect: JsObject): JsObject = {
    var shallowQueryAfterSelect = queryAfterSelect
    globalAggrOpt match {
      case Some(globalAggr) =>
        val aggr = globalAggr.aggregate
        val field = aggr.field.name
        val as = aggr.as.name
        val funcName = typeImpl.getAggregateStr(aggr.func)
        val aggregatedJson = Json.parse(s"""{"func": "$funcName", "as": "$as"}""")
        globalAggr.aggregate.func match {
          case Count => {
            shallowQueryAfterSelect += {"aggregation" -> aggregatedJson}
            if (field != "*") {
              shallowQueryAfterSelect += ("_source" -> Json.arr(JsString(field)))
            }
          }
          case Min | Max => {
            shallowQueryAfterSelect += ("aggregation" -> aggregatedJson)
            shallowQueryAfterSelect += ("size" -> JsNumber(0))
            shallowQueryAfterSelect += ("aggs" -> Json.obj( as -> Json.obj(funcName -> Json.obj("field" -> JsString(field)))))
          }
        }
      case None => return shallowQueryAfterSelect
    }
    shallowQueryAfterSelect
  }

  private def initExprMap(dataset: String, schemaMap: Map[String, AbstractSchema]): Map[String, FieldExpr] = {
    // Type of val schema: Schema
    val schema = schemaMap(dataset)
    schema.fieldMap.mapValues { f =>
      f.dataType match {
        case DataType.Record => FieldExpr(allFieldVar, allFieldVar)
        case DataType.Hierarchy => FieldExpr(allFieldVar, allFieldVar)
        case _ => { FieldExpr(f.name, f.name) }
      }
    }
  }

  private def parseFilterRelation(filter: FilterStatement, fieldExpr: String): String = {
    filter.field.dataType match {
      case DataType.Time => parseTimeRelation(filter, fieldExpr)
      case DataType.Text => parseTextRelation(filter, fieldExpr)
      case DataType.Number => parseNumberRelation(filter, fieldExpr)
      case DataType.String => parseStringRelation(filter, fieldExpr)
      case DataType.Hierarchy => throw new QueryParsingException("the Hierarchy type doesn't support any relation.")
      case _ => throw new QueryParsingException(s"unknown datatype: ${ filter.field.dataType }")
    }
  }

  private def parseNumberRelation(filter: FilterStatement, fieldExpr: String): String = {
    filter.relation match {
      case Relation.inRange =>
        if (filter.values.size != 2) throw new QueryParsingException(s"relation: ${filter.relation} requires two parameters")
        s"""{"range": {"$fieldExpr": {"gte": ${filter.values(0)}, "lt": ${filter.values(1)}}}}"""
      case Relation.in => {
        joinTermsFilter = filter.values.asInstanceOf[Seq[Int]]
        s"""{"terms": {"$fieldExpr": [${filter.values.mkString(",")}]}}"""
      }
      case Relation.< =>
        s"""{"range": {"$fieldExpr": {"lt": ${filter.values(0)}}}}"""
      case Relation.> =>
        s"""{"range": {"$fieldExpr": {"gt": ${filter.values(0)}}}}"""
      case Relation.<= =>
        s"""{"range": {"$fieldExpr": {"lte": ${filter.values(0)}}}}"""
      case Relation.>= =>
        s"""{"range": {"$fieldExpr": {"gte": ${filter.values(0)}}}}"""
      case Relation.== =>
        s"""{"match": {"$fieldExpr": ${filter.values(0)}}}"""
      case _ => throw new QueryParsingException("no supported parameter for this number in Elasticsearch")
    }
  }

  private def parseTimeRelation(filter: FilterStatement, fieldExpr: String): String = {
    filter.relation match {
      case Relation.inRange =>
        s"""{"range": {"$fieldExpr": {"gte": "${filter.values(0)}", "lt": "${filter.values(1)}"}}}"""
      case Relation.< =>
        s"""{"range": {"$fieldExpr": {"lt": "${filter.values(0)}"}}}"""
      case Relation.> =>
        s"""{"range": {"$fieldExpr": {"gt": "${filter.values(0)}"}}}"""
      case Relation.<= =>
        s"""{"range": {"$fieldExpr": {"lte": "${filter.values(0)}"}}}"""
      case Relation.>= =>
        s"""{"range": {"$fieldExpr": {"gte": "${filter.values(0)}"}}}"""
      case _ => throw new QueryParsingException("no supported parameter for this date in Elasticsearch")
    }
  }

  private def parseStringRelation(filter: FilterStatement, fieldExpr: String): String = {
    filter.relation match {
      case Relation.matches => {
        val values = filter.values.map(_.asInstanceOf[String])
        s"""{"match": {"$fieldExpr": "${values(0)}"}}"""
      }
      case Relation.!= => throw new QueryParsingException("filter relation: !=, not implemented in Elasticsearch adapter")
      case Relation.contains => throw new QueryParsingException("filter relation: contains, not implemented in Elasticsearch adapter")
    }
  }

  private def parseTextRelation(filter: FilterStatement, fieldExpr: String): String = {
    val words = filter.values.map(w => s"${w.asInstanceOf[String].trim()}").mkString("\"", ",", "\"")
    s"""{"match": {"${fieldExpr}": {"query": $words, "operator": "and"}}}"""
  }

  private def parseIntervalDuration(interval: Interval): String = {
    import TimeUnit._
    interval.unit match {
      case Second => "second"
      case Minute => "minute"
      case Hour => "hour"
      case Day => "day"
      case Week => "week"
      case Month => "month"
      case Year => "year"
    }
  }

  private def parseGroupByFunc(by: ByStatement, fieldExpr: String, groupStr: StringBuilder, isLookUp: Boolean, selectOpt: Option[SelectStatement]): Unit = {
    by.funcOpt match {
      case Some(func) =>
        func match {
          case bin: Bin => ???
          case interval: Interval =>
            val duration = parseIntervalDuration(interval)
            groupStr.append(s""" "date_histogram": {"field": "${fieldExpr}", "interval": "$duration"} """.stripMargin)
          case level: Level => {
            val hierarchyField = by.field.asInstanceOf[HierarchyField]
            val field = hierarchyField.levels.find(_._1 == level.levelTag).get._2
            if (isLookUp) {
              groupStr.append(s""" "terms": {"field": "${field}", "size": 2147483647} """.stripMargin)
            } else {
              groupStr.append(s""" "terms": {"field": "${field}", "size": 2147483647, "order": {"_key":"asc"}} """.stripMargin)
            }
          }
          case _ => throw new QueryParsingException(s"unknown function: ${func.name}")
        }
      case None => {
        val orderStr = if (selectOpt.get.order.head == SortOrder.DSC) "desc" else "asc"
        groupStr.append(s""" "terms": {"field": "${fieldExpr}.keyword", "size": ${selectOpt.get.limit}, "order": {"_count": "$orderStr"}} """)
      }
    }
  }
}

object ElasticsearchGenerator extends IQLGeneratorFactory {
  override def apply(): IQLGenerator = new ElasticsearchGenerator()
}