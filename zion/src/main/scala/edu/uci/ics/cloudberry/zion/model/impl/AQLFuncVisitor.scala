package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.QueryParsingException
import edu.uci.ics.cloudberry.zion.model.schema.Relation.Relation
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.format.DateTimeFormat

import scala.reflect.runtime.universe.{TypeTag, typeOf}

object AQLFuncVisitor {

  val TimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

  def translateRelation(field: Field,
                        funcOpt: Option[TransformFunc],
                        sourceVar: String,
                        relation: Relation,
                        values: Seq[Any]
                       ): String = {
    //TODO add the function handling logic
    if (!Schema.Type2Relations.get(field.dataType).get.contains(relation)) {
      throw QueryParsingException(s"field ${field.name} of type ${field.dataType} can not apply to relation: ${relation}")
    }
    field.dataType match {
      case DataType.Number =>
        validateNumberValue(relation, values)
        translateNumberRelation(field, funcOpt, sourceVar, relation, values.map(_.asInstanceOf[AnyVal]))
      case DataType.Time =>
        validateTimeValue(relation, values)
        translateTimeRelation(field, funcOpt, sourceVar, relation, values.map(_.asInstanceOf[String]))
      case DataType.Point =>
        validatePointValue(relation, values)
        translatePointRelation(field, funcOpt, sourceVar, relation,
                               values.map(_.asInstanceOf[Seq[Number]].map(_.doubleValue())))
      case DataType.Boolean => ???
      case DataType.String => ???
      case DataType.Text =>
        validateTextValue(relation, values)
        translateTextRelation(field, funcOpt, sourceVar, relation, values.map(_.asInstanceOf[String]))
      case DataType.Bag => ???
      case DataType.Hierarchy =>
        throw QueryParsingException("the Hierarchy type doesn't support any relations.")
      case _ => throw QueryParsingException(s"unknown datatype: ${field.dataType}")
    }
  }

  private def translateNumberRelation(field: Field,
                                      funcOpt: Option[TransformFunc],
                                      sourceVar: String,
                                      relation: Relation,
                                      values: Seq[AnyVal]): String = {
    relation match {
      case Relation.inRange => {
        if (values.size != 2) throw QueryParsingException(s"relation: ${relation} require two parameters")
        return s"$sourceVar.${field.name} >= ${values(0)} and $sourceVar.${field.name} < ${values(1)}"
      }
      case Relation.in => {
        val setVar = s"${sourceVar}s"
        val ret =
          s"""|for $setVar in [ ${values.mkString(",")} ]
              |where $sourceVar.${field.name} = $setVar
              |""".stripMargin
        return ret
      }
      case _ => {
        if (values.size != 1) throw QueryParsingException(s"relation: ${relation} require one parameter")
        return s"$sourceVar.${field.name} $relation ${values.head}"
      }
    }
  }

  private def translateTimeRelation(field: Field,
                                    funcOpt: Option[TransformFunc],
                                    sourceVar: String,
                                    relation: Relation,
                                    values: Seq[String]): String = {
    relation match {
      case Relation.inRange => {
        if (values.size != 2) throw QueryParsingException(s"relation: ${relation} require two parameters")
        return s"$sourceVar.${field.name} >= datetime('${values(0)}') " +
          s"and $sourceVar.${field.name} < datetime('${values(1)}')"
      }
      case _ => {
        if (values.size != 1) throw QueryParsingException(s"relation: ${relation} require one parameter")
        s"$sourceVar.${field.name} $relation datetime('${values.head}')"
      }
    }
  }

  private def translatePointRelation(field: Field,
                                     funcOpt: Option[TransformFunc],
                                     sourceVar: String,
                                     relation: Relation,
                                     values: Seq[Seq[Double]]): String = {
    relation match {
      case Relation.inRange => {
        s"""
           |spatial-intersect($sourceVar.${field.name},
           |  create-rectangle(create-point(${values(0)(0)},${values(0)(1)}),
           |  create-point(${values(1)(0)},${values(1)(1)})))
           |""".stripMargin
      }
      case _ => throw QueryParsingException(s"point type doesn't support relation $relation")
    }
  }

  private def translateTextRelation(field: Field,
                                    funcOpt: Option[TransformFunc],
                                    sourceVar: String,
                                    relation: Relation,
                                    values: Seq[String]): String = {
    val first = s"similarity-jaccard(word-tokens($sourceVar.'${field.name}'), word-tokens('${values.head}')) > 0.0"
    val rest = values.tail.map(
      keyword => s"""and contains($sourceVar.'${field.name}', "$keyword")"""
    )
    (first +: rest).mkString("\n")
  }

  private def validateNumberValue(relation: Relation, values: Seq[Any]): Unit = {
    if (!values.forall(isAnyVal)) {
      throw QueryParsingException(s"values contain non compatible data type for relation: ${relation}")
    }
  }

  private def isAnyVal[T: TypeTag](t: T): Boolean = {
    implicitly[TypeTag[T]].tpe <:< typeOf[AnyVal]
  }

  private def validateTimeValue(relation: Relation, values: Seq[Any]) = {
    //required string format : http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html
    try {
      if (!values.forall(_.isInstanceOf[String])) {
        throw new IllegalArgumentException
      }
      // This parseDateTime will throw an exception if the format is invalid
      values.foreach(t => TimeFormat.parseDateTime(t.asInstanceOf[String]))
    } catch {
      case ex: IllegalArgumentException => throw QueryParsingException("invalid time format")
    }
  }

  private def validatePointValue(relation: Relation, values: Seq[Any]) = {
    //TODO support circle and polygon
    if (!values.forall(_.isInstanceOf[Seq[_]]) || values.size != 2
      || !values.map(_.asInstanceOf[Seq[_]]).forall(ary => ary.size == 2 && ary.forall(isAnyVal))) {
      throw QueryParsingException(s"the ${relation} on point type requires a pair of value pairs")
    }
  }

  private def validateTextValue(relation: Relation, values: Seq[Any]) = {
    if (!values.forall(_.isInstanceOf[String])) {
      throw QueryParsingException(s"the ${relation} on text type requires string parameters")
    }
  }

  def translateGroupFunc(field: Field,
                         funcOpt: Option[GroupFunc],
                         sourceVar: String
                        ): String = {
    funcOpt.map { func =>
      IFunction.verifyField(func, field).map { msg => throw QueryParsingException(msg) }
      func match {
        case bin: Bin => ???
        case interval: Interval =>
          import TimeUnit._
          //PnYnMnDTnHnMn.mmmS
          val duration = interval.unit match {
            case Second => s""" day-time-duration("PT${interval.x}S") """
            case Minute => s""" day-time-duration("PT${interval.x}M") """
            case Hour => s""" day-time-duration("PT${interval.x}H") """
            case Day => s""" day-time-duration("P${interval.x}D") """
            case Week => s""" day-time-duration("P${interval.x * 7}D") """
            case Month => s""" year-month-duration("P${interval.x}M") """
            case Year => s""" year-month-duration("P${interval.x}Y") """
          }
          s"get-interval-start-datetime(interval-bin($sourceVar.${field.name}, datetime('1990-01-01T00:00:00.000Z'), $duration))"
        case level: Level => ???
        case GeoCellTenth => ???
        case GeoCellHundredth => ???
        case GeoCellThousandth => ???
        case _ => throw QueryParsingException(s"unknown function: ${func.name}")
      }
    }.getOrElse(s"$sourceVar.${field.name}")
  }

  def translateAggrFunc(field: Field,
                        funcOpt: Option[IFunction],
                        sourceVar: String
                       ): String = ???

}
