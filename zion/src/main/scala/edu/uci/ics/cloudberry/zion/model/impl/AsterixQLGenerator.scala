package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.QueryParsingException
import edu.uci.ics.cloudberry.zion.model.schema._

trait AsterixTypeImpl extends TypeImpl {

  val datetime: String = "datetime"
  val round: String = "round"

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
  val contains: String
  val wordTokens: String

}

abstract class AsterixQueryGenerator extends AbstractQueryGenerator {

  case class FieldExpr(refExpr: String, defExpr: String)

  case class PartialResult(parts: Seq[String], exprMap: Map[String, FieldExpr])

  protected def quote: String

  protected def typeImpl: AsterixTypeImpl

  protected def sourceVar: String

  protected def lookupVar: String

  protected def unnestVar: String

  protected def groupVar: String

  protected def globalAggrVar: String

  protected def outerSelectVar: String


  def calcResultSchema(query: Query, schemaMap: Map[String, Schema]): Schema = {
    if (query.lookups.isEmpty && query.group.isEmpty && query.select.isEmpty) {
      schemaMap(query.dataset).copy()
    } else {
      ???
    }
  }


  protected def initExprMap(query: Query): Map[String, FieldExpr] = {
    query.fieldMap.mapValues { f =>
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
      case DataType.String => ???
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

  private def parseTimeRelation(filter: FilterStatement,
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


  private def parsePointRelation(filter: FilterStatement,
                                 fieldExpr: String): String = {
    val values = filter.values.map(_.asInstanceOf[Seq[Double]])
    filter.relation match {
      case Relation.inRange => {
        s"""
           |${typeImpl.spatialIntersect}($fieldExpr,
           |  ${typeImpl.createRectangle}(${typeImpl.createPoint}(${values(0)(0)},${values(0)(1)}),
           |  ${typeImpl.createPoint}(${values(1)(0)},${values(1)(1)})))
           |""".stripMargin
      }
    }
  }

  private def parseTextRelation(filter: FilterStatement,
                                fieldExpr: String): String = {
    val first = s"${typeImpl.similarityJaccard}(${typeImpl.wordTokens}($fieldExpr), ${typeImpl.wordTokens}('${filter.values.head}')) > 0.0"
    val rest = filter.values.tail.map(keyword => s"""and ${typeImpl.contains}($fieldExpr, "$keyword")""")
    (first +: rest).mkString("\n")
  }


  private def parseGeoCell(scale: Double, fieldExpr: String, dataType: DataType.Value): String = {
    val origin = s"${typeImpl.createPoint}(0.0,0.0)"
    s"${typeImpl.getPoints}(${typeImpl.spatialCell}(${fieldExpr}, $origin, ${1 / scale}, ${1 / scale}))[0]"
  }

  protected def parseAggregateFunc(aggregate: AggregateStatement,
                                   fieldExpr: String): String


  private def parseIntervalDuration(interval: Interval): String = {
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
    if (groupBy.funcOpt.isEmpty) {
      return fieldExpr
    }
    val func = groupBy.funcOpt.get

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
      case DataType.Bag => s"{{${fieldType2ADMType(new Field("", field.asInstanceOf[BagField].innerType))}}}"
      case DataType.Hierarchy => ??? // should be skipped
      case DataType.Record => ???
    }
  }
}
