package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.{IQLGenerator, QueryParsingException}
import edu.uci.ics.cloudberry.zion.model.schema._


/**
  * Defines constant strings for query languages supported by AsterixDB
  */
trait AsterixImpl {

  val aggregateFuncMap: Map[AggregateFunc, String]

  def getAggregateStr(aggregate: AggregateFunc): String = {
    aggregateFuncMap.get(aggregate) match {
      case Some(impl) => impl
      case None => throw new QueryParsingException(s"No implementation is provided for aggregate function ${aggregate.name}")
    }
  }

  val datetime: String
  val round: String

  val dayTimeDuration: String
  val yearMonthDuration: String
  val getIntervalStartDatetime: String
  val intervalBin: String

  val spatialIntersect: String
  val createRectangle: String
  val createPoint: String
  val spatialCell: String
  val getPoints: String


  val similarityJaccard: String
  val fullTextContains: String
  val contains: String
  val wordTokens: String

}

object AsterixImpl {


}


abstract class AsterixQueryGenerator extends IQLGenerator {

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

  protected def typeImpl: AsterixImpl

  protected def quote: String

  protected def sourceVar: String

  protected def appendVar: String

  protected def lookupVar: String

  protected def unnestVar: String

  protected def groupVar: String

  protected def groupedLookupVar: String

  protected def groupedLookupSourceVar: String

  protected def globalAggrVar: String

  protected def outerSelectVar: String

  /**
    * The suffix (such as ";") appended to the query string
    *
    * @return
    */
  protected def suffix: String


  /**
    * Returns a query string query after parsing the query object.
    *
    * @param query     [[IQuery]] object containing query details
    * @param schemaMap a map of Dataset name to it's [[AbstractSchema]]
    * @return query string
    **/
  def generate(query: IQuery, schemaMap: Map[String, AbstractSchema]): String = {
    val (temporalSchemaMap, lookupSchemaMap) = GeneratorUtil.splitSchemaMap(schemaMap)
    val result = query match {
      case q: Query =>
        parseQuery(q, temporalSchemaMap)
      case q: CreateView => parseCreate(q, temporalSchemaMap)
      case q: AppendView => parseAppend(q, temporalSchemaMap)
      case q: UpsertRecord => parseUpsert(q, schemaMap)
      case q: DropView => parseDrop(q, schemaMap)
      case q: DeleteRecord => parseDelete(q, schemaMap)
      case _ => ???
    }
    s"$result$suffix"
  }

  protected def parseQuery(query: Query, schemaMap: Map[String, Schema]): String

  protected def parseCreate(query: CreateView, schemaMap: Map[String, Schema]): String

  protected def parseAppend(query: AppendView, schemaMap: Map[String, Schema]): String

  protected def parseUpsert(query: UpsertRecord, schemaMap: Map[String, AbstractSchema]): String

  protected def parseDelete(query: DeleteRecord, schemaMap: Map[String, AbstractSchema]): String

  protected def parseDrop(query: DropView, schemaMap: Map[String, AbstractSchema]): String

  def calcResultSchema(query: Query, schema: Schema): Schema = {
    if (query.lookup.isEmpty && query.groups.isEmpty && query.select.isEmpty) {
      schema.copySchema
    } else {
      ???
    }
  }

  protected def initExprMap(dataset: String, schemaMap: Map[String, AbstractSchema]): Map[String, FieldExpr] = {
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


  //TODO possibly using /*+ skip-index */ hint if the relation selectivity is not high enough
  protected def parseFilterRelation(filter: FilterStatement, fieldExpr: String): String = {
    filter.field.dataType match {
      case DataType.Number =>
        parseNumberRelation(filter, fieldExpr)
      case DataType.Time =>
        parseTimeRelation(filter, fieldExpr)
      case DataType.Point =>
        parsePointRelation(filter, fieldExpr)
      case DataType.Boolean => ???
      case DataType.String => parseStringRelation(filter, fieldExpr)
      case DataType.Text =>
        parseTextRelation(filter, fieldExpr)
      case DataType.Bag => ???
      case DataType.Hierarchy =>
        throw new QueryParsingException("the Hierarchy type doesn't support any relations.")
      case _ => throw new QueryParsingException(s"unknown datatype: ${filter.field.dataType}")
    }
  }


  protected def parseNumberRelation(filter: FilterStatement,
                                    fieldExpr: String): String

  protected def parseTimeRelation(filter: FilterStatement,
                                  fieldExpr: String): String = {
    filter.relation match {
      case Relation.inRange => {
        s"$fieldExpr >= ${typeImpl.datetime}('${filter.values(0)}') and $fieldExpr < ${typeImpl.datetime}('${filter.values(1)}')"
      }
      case _ => {
        s"$fieldExpr ${filter.relation} ${typeImpl.datetime}('${filter.values(0)}')"
      }
    }
  }


  protected def parsePointRelation(filter: FilterStatement,
                                   fieldExpr: String): String = {
    val values = filter.values.map(_.asInstanceOf[Seq[Double]])
    filter.relation match {
      case Relation.inRange => {
        s"""${typeImpl.spatialIntersect}($fieldExpr,
           |  ${typeImpl.createRectangle}(${typeImpl.createPoint}(${values(0)(0)},${values(0)(1)}),
           |  ${typeImpl.createPoint}(${values(1)(0)},${values(1)(1)})))
           |""".stripMargin
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
    val words = filter.values.map(w => s"'${w.asInstanceOf[String].trim}'").mkString("[", ",", "]")
    s"${typeImpl.fullTextContains}($fieldExpr, $words, {'mode':'all'})"
  }


  protected def parseGeoCell(scale: Double, fieldExpr: String, dataType: DataType.Value): String = {
    val origin = s"${typeImpl.createPoint}(0.0,0.0)"
    s"${typeImpl.getPoints}(${typeImpl.spatialCell}(${fieldExpr}, $origin, ${1 / scale}, ${1 / scale}))[0]"
  }

  protected def parseAggregateFunc(aggregate: AggregateStatement,
                                   fieldExpr: String): String


  protected def parseIntervalDuration(interval: Interval): String = {
    import TimeUnit._
    //PnYnMnDTnHnMn.mmmS
    interval.unit match {
      case Second => s""" ${typeImpl.dayTimeDuration}("PT${interval.x}S") """
      case Minute => s""" ${typeImpl.dayTimeDuration}("PT${interval.x}M") """
      case Hour => s""" ${typeImpl.dayTimeDuration}("PT${interval.x}H") """
      case Day => s""" ${typeImpl.dayTimeDuration}("P${interval.x}D") """
      case Week => s""" ${typeImpl.dayTimeDuration}("P${interval.x * 7}D") """
      case Month => s""" ${typeImpl.yearMonthDuration}("P${interval.x}M") """
      case Year => s""" ${typeImpl.yearMonthDuration}("P${interval.x}Y") """
    }
  }

  protected def parseGroupByFunc(groupBy: ByStatement, fieldExpr: String): String = {
    groupBy.funcOpt match {
      case Some(func) =>
        func match {
          case bin: Bin => s"${typeImpl.round}($fieldExpr/${bin.scale})*${bin.scale}"
          case interval: Interval =>
            val duration = parseIntervalDuration(interval)
            s"${typeImpl.getIntervalStartDatetime}(${typeImpl.intervalBin}($fieldExpr, ${typeImpl.datetime}('1990-01-01T00:00:00.000Z'), $duration))"
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


  protected def genDDL(schema: AbstractSchema): String = {
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
       |create type ${schema.getTypeName} if not exists as open {
       |${fields.mkString(",\n")}
       |};
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
}
