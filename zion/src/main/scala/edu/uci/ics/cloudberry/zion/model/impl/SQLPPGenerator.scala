package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{ FieldNotFound, IQLGenerator, IQLGeneratorFactory, QueryParsingException }
import edu.uci.ics.cloudberry.zion.model.schema._
import play.api.libs.json.Json

import scala.collection.mutable

/**
 * Created by luochen on 2017/2/19.
 */
class SQLPPGenerator extends IQLGenerator {

  val sourceVar = "t"

  val groupVar = "g"

  /**
   * Parser the Query to string statements.
   *
   * @param query
   * @param schema
   * @return
   */
  def generate(query: IQuery, schema: Schema): String = {
    query match {
      case q: Query =>
        parseQuery(q, schema) + ";"
      case q: CreateView => parseCreate(q, schema) + ";"
      case q: AppendView => parseAppend(q, schema) + ";"
      case q: UpsertRecord => parseUpsert(q, schema) + ";"
      case q: DropView => ???
      case _ => ???
    }
  }

  def calcResultSchema(query: Query, schema: Schema): Schema = {
    if (query.lookups.isEmpty && query.group.isEmpty && query.select.isEmpty) {
      schema.copy()
    } else {
      ???
    }
  }

  def parseCreate(create: CreateView, sourceSchema: Schema): String = {
    val resultSchema = calcResultSchema(create.query, sourceSchema)
    val ddl: String = genDDL(resultSchema)
    val createDataSet =
      s"""
         |drop dataset ${create.datasetName} if exists;
         |create dataset ${create.datasetName}(${resultSchema.typeName}) primary key ${resultSchema.primaryKey.mkString(",")} //with filter on '${resultSchema.timeField}'
         |""".stripMargin
    val insert =
      s"""
         |insert into ${create.datasetName} (
         |${parseQuery(create.query, sourceSchema)}
         |)
       """.stripMargin
    ddl + createDataSet + insert
  }

  def parseAppend(append: AppendView, sourceSchema: Schema): String = {
    s"""
       |upsert into ${append.datasetName} (
       |${parseQuery(append.query, sourceSchema)}
       |)
     """.stripMargin
  }

  def parseUpsert(q: UpsertRecord, schema: Schema): String = {
    s"""
       |upsert into ${q.datasetName} (
       |${Json.toJson(q.records)}
       |)
     """.stripMargin
  }

  def parseQuery(query: Query, schema: Schema): String = {

    val varMap: Map[String, SQLPPVar] = schema.fieldMap.mapValues { f =>
      f.dataType match {
        case DataType.Record => SQLPPVar(f, sourceVar)
        case DataType.Hierarchy => SQLPPVar(f, sourceVar) // TODO rethink this type: a type or just a relation between types?
        case _ => {
          //Add the quote to wrap the name in order to not touch the SQL reserved keyword
          val addQuote = f.name.split('.').map(name => s"`$name`").mkString(".")
          SQLPPVar(f, s"$sourceVar.$addQuote")
        }
      }
    }

    val (lookupSQL, varMapAfterLookup) = parseLookup(query.lookups, varMap)
    val fromSQL = s"from ${query.datasetName} $sourceVar $lookupSQL".trim

    val (unnestSQL, varMapAfterUnnest, unnestTests) = parseUnnest(query.unnests, varMapAfterLookup)

    val filterSQL = parseFilter(query.filters, varMapAfterLookup, unnestTests)

    val (groupSQL, varMapAfterGroup, projectVars) = query.group.map(parseGroupby(_, varMapAfterUnnest, groupVar))
      .getOrElse(("", varMapAfterUnnest, Map.empty[String, String]))

    val (projectSQL, orderSQL, limitSQL, offsetSQL, varMapAfterSelect) = query.select.map(parseSelect(_, varMapAfterGroup, projectVars, !query.unnests.isEmpty))
      .getOrElse((parseDefaultProject(projectVars, varMapAfterGroup, !query.unnests.isEmpty), "", "", "", varMapAfterGroup))

    val sql = Seq(
      projectSQL,
      fromSQL,
      unnestSQL,
      filterSQL,
      groupSQL,
      orderSQL,
      limitSQL,
      offsetSQL).filter(!_.isEmpty).mkString("\n")

    val globalAggrVar = "c"

    val (finalSQL, varMapAfterGlobalAggr) = query.globalAggr.map(parseGlobalAggr(_, varMapAfterSelect, sql, globalAggrVar))
      .getOrElse((sql, varMapAfterSelect))
    return finalSQL
  }

  private def parseLookup(lookups: Seq[LookupStatement],
    varMap: Map[String, SQLPPVar]): (String, Map[String, SQLPPVar]) = {
    val sb = StringBuilder.newBuilder
    val producedVar = mutable.Map.newBuilder[String, SQLPPVar]
    lookups.zipWithIndex.foreach {
      case (lookup, id) =>
        val lookupVar = s"l$id"
        val keyZip = lookup.lookupKeys.zip(lookup.sourceKeys).map {
          case (lookupKey, sourceKey) =>
            varMap.get(sourceKey) match {
              case Some(sqlVar) => s"$lookupVar.$lookupKey = ${sqlVar.sqlExpr}"
              case None => throw FieldNotFound(sourceKey)
            }
        }
        sb.append(
          s"""
             |left outer join ${lookup.datasetName} $lookupVar on
             |where ${keyZip.mkString(" and ")}
        """.stripMargin)
        //TODO check if the vars are duplicated
        val field: Field = ??? // get field from lookup table
        producedVar ++= lookup.as.zip(lookup.selectValues).map { p =>
          p._1 -> SQLPPVar(new Field(p._1, field.dataType), s"$lookupVar.${p._2}")
        }
    }
    (sb.toString(), (producedVar ++= varMap).result().toMap)
  }

  private def parseFilter(filters: Seq[FilterStatement], varMap: Map[String, SQLPPVar], optionalUnnestTests: List[String]): String = {
    if (filters.isEmpty && optionalUnnestTests.isEmpty) return ""
    val filterStrs = filters.map { filter =>
      varMap.get(filter.fieldName) match {
        case Some(variable) =>
          SQLPPFuncVisitor.translateRelation(variable.field, filter.funcOpt, variable.sqlExpr, filter.relation, filter.values)
        case None => throw FieldNotFound(filter.fieldName)
      }
    }
    (optionalUnnestTests ++ filterStrs).mkString("where ", " and ", "")
  }

  private def parseUnnest(unnest: Seq[UnnestStatement],
    varMap: Map[String, SQLPPVar]): (String, Map[String, SQLPPVar], List[String]) = {
    val producedVar = mutable.Map.newBuilder[String, SQLPPVar]
    val optionalList = List.newBuilder[String]
    val sql = unnest.zipWithIndex.map {
      case (stat, id) =>
        varMap.get(stat.fieldName) match {
          case Some(sqlVar) =>
            sqlVar.field match {
              case field: BagField =>
                val newVar = s"`unnest${id}`"
                producedVar += stat.as -> SQLPPVar(new Field(stat.as, field.innerType), newVar)
                if (field.isOptional) {
                  optionalList += s"not(is_null(${sqlVar.sqlExpr}))"
                }
                s"unnest ${sqlVar.sqlExpr} $newVar"
              case _ => throw new QueryParsingException("unnest can only apply on Bag type")
            }
          case None => throw FieldNotFound(stat.fieldName)
        }
    }.mkString("\n")
    (sql, (producedVar ++= varMap).result().toMap, optionalList.result())
  }

  private def parseGroupby(group: GroupStatement,
    varMap: Map[String, SQLPPVar],
    varGroupSource: String = "g"): (String, Map[String, SQLPPVar], Map[String, String]) = {
    val producedVar = mutable.Map.newBuilder[String, SQLPPVar]
    val projects = Map.newBuilder[String, String]
    val groups: Seq[String] = group.bys.zipWithIndex.map {
      case (by, id) =>
        varMap.get(by.fieldName) match {
          case Some(sqlVar) =>
            val key = by.as.getOrElse(sqlVar.field.name)
            val (dataType, sqlGroupExpr) = SQLPPFuncVisitor.translateGroupFunc(sqlVar.field, by.funcOpt, sqlVar.sqlExpr)
            producedVar += key -> SQLPPVar(new Field(key, dataType), s"$varGroupSource.$key")
            val groupSQL = (s"$sqlGroupExpr as `$key`")
            projects += key -> s"`${key}`"
            groupSQL
          case None => throw FieldNotFound(by.fieldName)
        }
    }
    val groupBySQL = s"group by ${groups.mkString(",")} group as ${varGroupSource}"

    group.aggregates.foreach { aggr =>
      varMap.get(aggr.fieldName) match {
        case Some(sqlVar) =>
          val (dataType, sqlAggExpr) = SQLPPFuncVisitor.translateAggrFunc(sqlVar.field, aggr.func,
            varGroupSource, sqlVar.sqlExpr, aggr.as)
          producedVar += aggr.as -> SQLPPVar(new Field(aggr.as, dataType), s"`${aggr.as}`")
          projects += aggr.as -> sqlAggExpr
        case None => throw FieldNotFound(aggr.fieldName)
      }
    }

    (groupBySQL, producedVar.result().toMap, projects.result())
  }

  private def parseSelect(select: SelectStatement,
    varMap: Map[String, SQLPPVar],
    projectVars: Map[String, String], unnested: Boolean): (String, String, String, String, Map[String, SQLPPVar]) = {

    val producedVar = mutable.Map.newBuilder[String, SQLPPVar]
    //sampling only
    val orders = select.orderOn.map { fieldNameWithOrder =>
      val order = if (fieldNameWithOrder.startsWith("-")) "desc" else ""
      val fieldName = if (fieldNameWithOrder.startsWith("-")) fieldNameWithOrder.substring(1) else fieldNameWithOrder
      varMap.get(fieldName) match {
        case Some(sqlVar) => s"${sqlVar.sqlExpr} $order"
        case None => throw FieldNotFound(fieldName)
      }
    }
    val orderSQL = if (orders.nonEmpty) orders.mkString("order by ", ",", "") else ""

    val projectSQL =
      if (select.fieldNames.isEmpty) {
        producedVar ++= varMap
        parseDefaultProject(projectVars, varMap, unnested)
      } else {
        val exprs = select.fieldNames.map { fieldName =>
          varMap.get(fieldName) match {
            case Some(sqlVar) =>
              producedVar += fieldName -> sqlVar
              if (projectVars.isEmpty) {
                s"${sqlVar.sqlExpr} as `$fieldName`"
              } else {
                s"${projectVars.get(fieldName).get} as `$fieldName`"
              }

            case None => throw FieldNotFound(fieldName)
          }
        }
        s"select ${exprs.mkString(",")}"
      }

    val limitSQL = s"limit ${select.limit}"
    val offsetSQL = s"offset ${select.offset}"

    (projectSQL, orderSQL, limitSQL, offsetSQL, producedVar.result().toMap)
  }

  private def parseDefaultProject(projectVars: Map[String, String], varMap: Map[String, SQLPPVar], unnested: Boolean): String = {
    if (projectVars.isEmpty) {
      //no group by operation
      if (unnested) {
        val fields = varMap.values.filter(_.field.name != "*").map(v => s"${v.sqlExpr} as `${v.field.name}`")
        s"select ${fields.mkString(",")}"
      } else {
        s"select value $sourceVar"
      }
    } else {
      s"select ${projectVars.values.mkString(",")}"
    }
  }

  /**
   *
   * @param globalAggr
   * @param varMap
   * @param globalAggrVar
   * @return String: Prefix containing AQL statement for the aggr function. e.g.: count( for $c in (
   *         String: wrap and return the prefix statement . e.g.: ) return $c )
   *         Map[String, AQLVar]: result variables map after aggregation.
   */
  private def parseGlobalAggr(globalAggr: GlobalAggregateStatement,
    varMap: Map[String, SQLPPVar],
    sql: String,
    globalAggrVar: String): (String, Map[String, SQLPPVar]) = {

    val producedVar = mutable.Map.newBuilder[String, SQLPPVar]
    val aggr = globalAggr.aggregate
    val (aggFuncName, returnVar) =
      varMap.get(aggr.fieldName) match {
        case Some(sqlVar) =>
          val (dataType, aggFuncName, returnVar) = SQLPPFuncVisitor.translateGlobalAggr(sqlVar.field, aggr.func, globalAggrVar)
          producedVar += aggr.as -> SQLPPVar(new Field(aggr.as, dataType), s"${globalAggrVar}.${aggr.as}")
          (aggFuncName, returnVar.replaceAll("'", "`"))
        case None => throw FieldNotFound(aggr.fieldName)
      }
    val result =
      s"""
         |select $aggFuncName(
         |(select value $returnVar from ($sql) as $globalAggrVar)
         |) as `${aggr.as}`""".stripMargin
    (result, producedVar.result().toMap)
  }

  private def genDDL(schema: Schema): String = {

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

  private def fieldType2ADMType(field: Field): String = {
    field.dataType match {
      case DataType.Number => "double"
      case DataType.Time => "datetime"
      case DataType.Point => "point"
      case DataType.Boolean => "boolean"
      case DataType.String => "string"
      case DataType.Text => "string"
      case DataType.Bag => s"{{${fieldType2ADMType(new Field("", field.asInstanceOf[BagField].innerType))}}}"
      case DataType.Hierarchy => ??? // should be skipped
      case DataType.Record => ???
    }
  }

  case class SQLPPVar(field: Field, sqlExpr: String)

}

object SQLPPGenerator extends IQLGeneratorFactory {
  override def apply(): IQLGenerator = new SQLPPGenerator()
}
