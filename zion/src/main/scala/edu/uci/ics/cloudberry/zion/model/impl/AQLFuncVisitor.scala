package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.QueryParsingException
import edu.uci.ics.cloudberry.zion.model.schema.Relation.Relation
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.format.DateTimeFormat

import scala.reflect.runtime.universe.{TypeTag, typeOf}

object AQLFuncVisitor {

  val TimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")

  //TODO possibly using /*+ skip-index */ hint if the relation selectivity is not high enough
  def translateRelation(field: Field,
                        funcOpt: Option[TransformFunc],
                        aqlExpr: String,
                        relation: Relation,
                        values: Seq[Any]
                       ): String = {
    //TODO add the function handling logic
    if (!Schema.Type2Relations(field.dataType).contains(relation)) {
      throw new QueryParsingException(s"field ${field.name} of type ${field.dataType} can not apply to relation: ${relation}")
    }
    field.dataType match {
      case DataType.Number =>
        validateNumberValue(relation, values)
        translateNumberRelation(field, funcOpt, aqlExpr, relation, values.map(_.asInstanceOf[AnyVal]))
      case DataType.Time =>
        validateTimeValue(relation, values)
        translateTimeRelation(field, funcOpt, aqlExpr, relation, values.map(_.asInstanceOf[String]))
      case DataType.Point =>
        validatePointValue(relation, values)
        translatePointRelation(field, funcOpt, aqlExpr, relation,
                               values.map(_.asInstanceOf[Seq[Number]].map(_.doubleValue())))
      case DataType.Boolean => ???
      case DataType.String => ???
      case DataType.Text =>
        validateTextValue(relation, values)
        translateTextRelation(field, funcOpt, aqlExpr, relation, values.map(_.asInstanceOf[String]))
      case DataType.Bag => ???
      case DataType.Hierarchy =>
        throw new QueryParsingException("the Hierarchy type doesn't support any relations.")
      case _ => throw new QueryParsingException(s"unknown datatype: ${field.dataType}")
    }
  }

  private def translateNumberRelation(field: Field,
                                      funcOpt: Option[TransformFunc],
                                      aqlExpr: String,
                                      relation: Relation,
                                      values: Seq[AnyVal]): String = {
    relation match {
      case Relation.inRange =>
        if (values.size != 2) throw new QueryParsingException(s"relation: $relation require two parameters")
        s"$aqlExpr >= ${values(0)} and $aqlExpr < ${values(1)}"
      case Relation.in =>
        val setVar = s"$$set${field.name.replace('.', '_')}"
        s"""|true
            |for $setVar in [ ${values.mkString(",")} ]
            |where $aqlExpr = $setVar
            |""".stripMargin
      case _ =>
        if (values.size != 1) throw new QueryParsingException(s"relation: $relation require one parameter")
        s"$aqlExpr $relation ${values.head}"
    }
  }

  private def translateTimeRelation(field: Field,
                                    funcOpt: Option[TransformFunc],
                                    aqlExpr: String,
                                    relation: Relation,
                                    values: Seq[String]): String = {
    relation match {
      case Relation.inRange => {
        if (values.size != 2) throw new QueryParsingException(s"relation: $relation require two parameters")
        return s"$aqlExpr >= datetime('${values(0)}') " +
          s"and $aqlExpr < datetime('${values(1)}')"
      }
      case _ => {
        if (values.size != 1) throw new QueryParsingException(s"relation: $relation require one parameter")
        s"$aqlExpr $relation datetime('${values.head}')"
      }
    }
  }

  private def translatePointRelation(field: Field,
                                     funcOpt: Option[TransformFunc],
                                     aqlExpr: String,
                                     relation: Relation,
                                     values: Seq[Seq[Double]]): String = {
    relation match {
      case Relation.inRange => {
        s"""
           |spatial-intersect($aqlExpr,
           |  create-rectangle(create-point(${values(0)(0)},${values(0)(1)}),
           |  create-point(${values(1)(0)},${values(1)(1)})))
           |""".stripMargin
      }
      case _ => throw new QueryParsingException(s"point type doesn't support relation $relation")
    }
  }

  private def translateTextRelation(field: Field,
                                    funcOpt: Option[TransformFunc],
                                    aqlExpr: String,
                                    relation: Relation,
                                    values: Seq[String]): String = {
    val first = s"similarity-jaccard(word-tokens($aqlExpr), word-tokens('${values.head}')) > 0.0"
    val rest = values.tail.map(
      keyword => s"""and contains($aqlExpr, "$keyword")"""
    )
    (first +: rest).mkString("\n")
  }

  private def validateNumberValue(relation: Relation, values: Seq[Any]): Unit = {
    if (!values.forall(isAnyNumber)) {
      throw new QueryParsingException(s"values contain non compatible data type for relation: $relation")
    }
  }

  private def isAnyNumber[T: TypeTag](t: T): Boolean = {
    t.isInstanceOf[Number] || implicitly[TypeTag[T]].tpe <:< typeOf[AnyVal]
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
      case ex: IllegalArgumentException => throw new QueryParsingException("invalid time format")
    }
  }

  private def validatePointValue(relation: Relation, values: Seq[Any]) = {
    //TODO support circle and polygon
    if (!values.forall(_.isInstanceOf[Seq[_]]) || values.size != 2
      || !values.map(_.asInstanceOf[Seq[_]]).forall(ary => ary.size == 2 && ary.forall(isAnyNumber))) {
      throw new QueryParsingException(s"the ${relation} on point type requires a pair of value pairs")
    }
  }

  private def validateTextValue(relation: Relation, values: Seq[Any]) = {
    if (!values.forall(_.isInstanceOf[String])) {
      throw new QueryParsingException(s"the ${relation} on text type requires string parameters")
    }
  }

  def translateGroupFunc(field: Field,
                         funcOpt: Option[GroupFunc],
                         aqlExpr: String
                        ): (DataType.DataType, String) = {
    funcOpt.map { func =>
      IFunction.verifyField(func, field).map { msg => throw new QueryParsingException(msg) }
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
          (DataType.Time,
            s"get-interval-start-datetime(interval-bin($aqlExpr, datetime('1990-01-01T00:00:00.000Z'), $duration))"
            )
        case level: Level =>
          //The `aqlExpr` for Hierarchy type only has the $t part
          //TODO remove this data type
          val hierarchyField = field.asInstanceOf[HierarchyField]
          hierarchyField.levels.get(level.levelTag) match {
            case Some(name) => (hierarchyField.innerType, s"$aqlExpr.$name")
            case None => throw new QueryParsingException(s"could not find the level tag ${level.levelTag} in hierarchy field ${field.name}")
          }
        case GeoCellTenth => ???
        case GeoCellHundredth => ???
        case GeoCellThousandth => ???
        case _ => throw new QueryParsingException(s"unknown function: ${func.name}")
      }
    }.getOrElse(field.dataType, s"$aqlExpr")
  }

  def translateAggrFunc(field: Field,
                        func: AggregateFunc,
                        aqlExpr: String
                       ): (DataType.DataType, String) = {
    func match {
      case Count =>
        if (field.dataType != DataType.Record) throw new QueryParsingException("count requires to aggregate on the record bag")
        (DataType.Number, s"count($aqlExpr)")
      case Max => ???
      case Min => ???
      case topK: TopK => ???
      case Avg => ???
      case Sum => ???
      case DistinctCount => ???
    }
  }

}
