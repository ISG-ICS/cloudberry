package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.QueryParsingException
import edu.uci.ics.cloudberry.zion.model.schema.DataType._
import edu.uci.ics.cloudberry.zion.model.schema.Relation._
import edu.uci.ics.cloudberry.zion.model.schema.Relation
import edu.uci.ics.cloudberry.zion.model.schema.{Relation => _, _}

import scala.collection.mutable
import scala.reflect.runtime.universe.{TypeTag, typeOf}

abstract class AsterixFunction {


  val datetime: String = "datetime"

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

  val count: String
  val max: String
  val min: String
  val avg: String
  val sum: String
}


/**
  * provides common util functions for aql/sqlpp func visitor
  */
abstract class AsterixFuncVisitor {

  protected val functionNames: AsterixFunction

  //TODO possibly using /*+ skip-index */ hint if the relation selectivity is not high enough
  def translateRelation(field: Field,
                        funcOpt: Option[TransformFunc],
                        qlExpr: String,
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
        translateNumberRelation(field, funcOpt, qlExpr, relation, values.map(_.asInstanceOf[AnyVal]))
      case DataType.Time =>
        validateTimeValue(relation, values)
        translateTimeRelation(field, funcOpt, qlExpr, relation, values.map(_.asInstanceOf[String]))
      case DataType.Point =>
        validatePointValue(relation, values)
        translatePointRelation(field, funcOpt, qlExpr, relation,
          values.map(_.asInstanceOf[Seq[Number]].map(_.doubleValue())))
      case DataType.Boolean => ???
      case DataType.String => ???
      case DataType.Text =>
        validateTextValue(relation, values)
        translateTextRelation(field, funcOpt, qlExpr, relation, values.map(_.asInstanceOf[String]))
      case DataType.Bag => ???
      case DataType.Hierarchy =>
        throw new QueryParsingException("the Hierarchy type doesn't support any relations.")
      case _ => throw new QueryParsingException(s"unknown datatype: ${field.dataType}")
    }
  }


  def translateGroupFunc(field: Field,
                         funcOpt: Option[GroupFunc],
                         aqlExpr: String
                        ): (DataType.DataType, String) = {
    funcOpt.map { func =>
      IFunction.verifyField(func, field).map { msg => throw new QueryParsingException(msg) }
      func match {
        case bin: Bin =>
          (DataType.Number,
            s"round($aqlExpr/${bin.scale})*${bin.scale}"
            )
        case interval: Interval =>
          import TimeUnit._
          //PnYnMnDTnHnMn.mmmS
          val duration = interval.unit match {
            case Second => s""" ${functionNames.dayTimeDuration}("PT${interval.x}S") """
            case Minute => s""" ${functionNames.dayTimeDuration}("PT${interval.x}M") """
            case Hour => s""" ${functionNames.dayTimeDuration}("PT${interval.x}H") """
            case Day => s""" ${functionNames.dayTimeDuration}("P${interval.x}D") """
            case Week => s""" ${functionNames.dayTimeDuration}("P${interval.x * 7}D") """
            case Month => s""" ${functionNames.yearMonthDuration}("P${interval.x}M") """
            case Year => s""" ${functionNames.yearMonthDuration}("P${interval.x}Y") """
          }
          (DataType.Time,
            s"${functionNames.getIntervalStartDatetime}(${functionNames.intervalBin}($aqlExpr, ${functionNames.datetime}('1990-01-01T00:00:00.000Z'), $duration))")
        case level: Level =>
          //The `aqlExpr` for Hierarchy type only has the $t part
          //TODO remove this data type
          val hierarchyField = field.asInstanceOf[HierarchyField]
          hierarchyField.levels.find(_._1 == level.levelTag) match {
            case Some(name) => (hierarchyField.innerType, s"$aqlExpr.${name._2}")
            case None => throw new QueryParsingException(s"could not find the level tag ${level.levelTag} in hierarchy field ${field.name}")
          }
        case GeoCellTenth =>
          (DataType.Point, getGeocellString(10, aqlExpr, field.dataType))
        case GeoCellHundredth =>
          (DataType.Point, getGeocellString(100, aqlExpr, field.dataType))
        case GeoCellThousandth =>
          (DataType.Point, getGeocellString(1000, aqlExpr, field.dataType))

        case _ => throw new QueryParsingException(s"unknown function: ${func.name}")
      }
    }.getOrElse(field.dataType, s"$aqlExpr")
  }

  def translateGlobalAggr(field: Field,
                          func: AggregateFunc,
                          sourceVar: String
                         ): (DataType, String, String) = {
    func match {
      case Count =>
        if (field.dataType != DataType.Record) throw new QueryParsingException("count requires to aggregate on the record bag")
        (DataType.Number, functionNames.count, sourceVar)
      case Max =>
        if (field.dataType != DataType.Number && field.dataType != DataType.Time) {
          throw new QueryParsingException(s"Max requires to aggregate on numbers or times, type ${field.dataType} is given")
        }
        (DataType.Number, functionNames.max, s"$sourceVar.'${field.name}'")
      case Min =>
        if (field.dataType != DataType.Number && field.dataType != DataType.Time) {
          throw new QueryParsingException(s"Min requires to aggregate on numbers or times, type ${field.dataType} is given")
        }
        (DataType.Number, functionNames.min, s"$sourceVar.'${field.name}'")
      case topK: TopK => ???
      case Avg =>
        if (field.dataType != DataType.Number) throw new QueryParsingException("Avg requires to aggregate on numbers")
        (DataType.Number, functionNames.avg, s"$sourceVar.'${field.name}'")
      case Sum =>
        if (field.dataType != DataType.Number) throw new QueryParsingException("Sum requires to aggregate on numbers")
        (DataType.Number, functionNames.sum, s"$sourceVar.'${field.name}'")
      case DistinctCount => ???
    }
  }

  protected def translateNumberRelation(field: Field,
                                        funcOpt: Option[TransformFunc],
                                        qlExpr: String,
                                        relation: Relation,
                                        values: Seq[AnyVal]): String

  protected def translateTimeRelation(field: Field,
                                      funcOpt: Option[TransformFunc],
                                      qlExpr: String,
                                      relation: Relation,
                                      values: Seq[String]): String = {
    relation match {
      case Relation.inRange => {
        if (values.size != 2) throw new QueryParsingException(s"relation: $relation require two parameters")
        return s"$qlExpr >= ${functionNames.datetime}('${values(0)}') " +
          s"and $qlExpr < ${functionNames.datetime}('${values(1)}')"
      }
      case _ => {
        if (values.size != 1) throw new QueryParsingException(s"relation: $relation require one parameter")
        s"$qlExpr $relation ${functionNames.datetime}('${values.head}')"
      }
    }
  }


  protected def translatePointRelation(field: Field,
                                       funcOpt: Option[TransformFunc],
                                       qlExpr: String,
                                       relation: Relation,
                                       values: Seq[Seq[Double]]): String = {
    relation match {
      case Relation.inRange => {
        s"""
           |${functionNames.spatialIntersect}($qlExpr,
           |  ${functionNames.createRectangle}(${functionNames.createPoint}(${values(0)(0)},${values(0)(1)}),
           |  ${functionNames.createPoint}(${values(1)(0)},${values(1)(1)})))
           |""".stripMargin
      }
      case _ => throw new QueryParsingException(s"point type doesn't support relation $relation")
    }
  }

  protected def translateTextRelation(field: Field,
                                      funcOpt: Option[TransformFunc],
                                      qlExpr: String,
                                      relation: Relation,
                                      values: Seq[String]): String = {
    val first = s"${functionNames.similarityJaccard}(${functionNames.wordTokens}($qlExpr), ${functionNames.wordTokens}('${values.head}')) > 0.0"
    val rest = values.tail.map(
      keyword => s"""and ${functionNames.contains}($qlExpr, "$keyword")"""
    )
    (first +: rest).mkString("\n")
  }


  private def getGeocellString(scale: Double, aqlExpr: String, dataType: DataType.Value): String = {
    if (dataType != DataType.Point) throw new QueryParsingException("Geo-cell requires a point")
    val origin = s"${functionNames.createPoint}(0.0,0.0)"
    s"${functionNames.getPoints}(${functionNames.spatialCell}(${
      aqlExpr
    }, $origin, ${
      1 / scale
    }, ${
      1 / scale
    }))[0]"
  }

  protected def validateNumberValue(relation: Relation, values: Seq[Any]): Unit = {
    if (!values.forall(isAnyNumber)) {
      throw new QueryParsingException(s"values contain non compatible data type for relation: $relation")
    }
  }

  protected def isAnyNumber[T: TypeTag](t: T): Boolean = {
    t.isInstanceOf[Number] || implicitly[TypeTag[T]].tpe <:< typeOf[AnyVal]
  }

  protected def validateTimeValue(relation: Relation, values: Seq[Any]) = {
    //required string format : http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html
    try {
      if (!values.forall(_.isInstanceOf[String])) {
        throw new IllegalArgumentException
      }
      // This parseDateTime will throw an exception if the format is invalid
      values.foreach(t => TimeField.TimeFormat.parseDateTime(t.asInstanceOf[String]))
    } catch {
      case ex: IllegalArgumentException => throw new QueryParsingException("invalid time format")
    }
  }

  protected def validatePointValue(relation: Relation, values: Seq[Any]) = {
    //TODO support circle and polygon
    if (!values.forall(_.isInstanceOf[Seq[_]]) || values.size != 2
      || !values.map(_.asInstanceOf[Seq[_]]).forall(ary => ary.size == 2 && ary.forall(isAnyNumber))) {
      throw new QueryParsingException(s"the ${
        relation
      } on point type requires a pair of value pairs")
    }
  }

  protected def validateTextValue(relation: Relation, values: Seq[Any]) = {
    if (!values.forall(_.isInstanceOf[String])) {
      throw new QueryParsingException(s"the ${
        relation
      } on text type requires string parameters")
    }
  }

}


object AQLFuncVisitor extends AsterixFuncVisitor {


  val functionNames = new AsterixFunction {
    override val count: String = "count"
    override val wordTokens: String = "word-tokens"
    override val yearMonthDuration: String = "year-month-duration"
    override val max: String = "max"
    override val createRectangle: String = "create-rectangle"
    override val avg: String = "avg"
    override val spatialIntersect: String = "spatial-intersect"
    override val similarityJaccard: String = "similarity-jaccard"
    override val min: String = "min"
    override val sum: String = "sum"
    override val contains: String = "contains"
    override val getPoints: String = "get-points"
    override val getIntervalStartDatetime: String = "get-interval-start-datetime"
    override val createPoint: String = "create-point"
    override val spatialCell: String = "spatial-cell"
    override val dayTimeDuration: String = "day-time-duration"
    override val intervalBin: String = "interval-bin"
  }

  /**
    *
    * @param field
    * @param func
    * @param aqlExpr
    * @return DataType: DataType of the aggregated result
    *         String: AQL aggregate function string
    *         String: New AQL variable representing the field to be aggregate
    *         String: AQL assignment of the field to the new variable (let clause)
    */
  def translateAggrFunc(field: Field,
                        func: AggregateFunc,
                        aqlExpr: String
                       ): (DataType, String, String, String) = {
    val newvar = s"${aqlExpr.split('.')(0)}aggr";
    func match {
      case Count =>
        if (field.dataType != DataType.Record) throw new QueryParsingException("count requires to aggregate on the record bag")
        (DataType.Number, s"${functionNames.count}($newvar)", newvar, s"let $newvar := $aqlExpr")
      case Max =>
        if (field.dataType != DataType.Number) throw new QueryParsingException("Max requires to aggregate on numbers")
        (DataType.Number, s"${functionNames.max}($newvar)", newvar, s"let $newvar := $aqlExpr")
      case Min =>
        if (field.dataType != DataType.Number) throw new QueryParsingException("Min requires to aggregate on numbers")
        (DataType.Number, s"${functionNames.min}($newvar)", newvar, s"let $newvar := $aqlExpr")
      case topK: TopK => ???
      case Avg =>
        if (field.dataType != DataType.Number) throw new QueryParsingException("Avg requires to aggregate on numbers")
        (DataType.Number, s"${functionNames.avg}($newvar)", newvar, s"let $newvar := $aqlExpr")
      case Sum =>
        if (field.dataType != DataType.Number) throw new QueryParsingException("Sum requires to aggregate on numbers")
        (DataType.Number, s"${functionNames.sum}($newvar)", newvar, s"let $newvar := $aqlExpr")
      case DistinctCount => ???
    }
  }

  protected def translateNumberRelation(field: Field,
                                        funcOpt: Option[TransformFunc],
                                        aqlExpr: String,
                                        relation: Relation,
                                        values: Seq[AnyVal]): String = {
    relation match {
      case Relation.inRange =>
        if (values.size != 2) throw new QueryParsingException(s"relation: $relation require two parameters")
        s"$aqlExpr >= ${
          values(0)
        } and $aqlExpr < ${
          values(1)
        }"
      case Relation.in =>
        val setVar = s"$$set${
          field.name.replace('.', '_')
        }"
        s"""|true
            |for $setVar in [ ${
          values.mkString(",")
        } ]
            |where $aqlExpr = $setVar
            |""".stripMargin
      case _ =>
        if (values.size != 1) throw new QueryParsingException(s"relation: $relation require one parameter")
        s"$aqlExpr $relation ${
          values.head
        }"
    }
  }

}


object SQLPPFuncVisitor extends AsterixFuncVisitor {


  val functionNames = new AsterixFunction {
    override val count: String = "coll_count"
    override val wordTokens: String = "word_tokens"
    override val yearMonthDuration: String = "year_month_duration"
    override val max: String = "coll_max"
    override val createRectangle: String = "create_rectangle"
    override val avg: String = "coll_avg"
    override val spatialIntersect: String = "spatial_intersect"
    override val similarityJaccard: String = "similarity_jaccard"
    override val min: String = "coll_min"
    override val sum: String = "coll_sum"
    override val contains: String = "contains"
    override val getPoints: String = "get_points"
    override val getIntervalStartDatetime: String = "get_interval_start_datetime"
    override val createPoint: String = "create_point"
    override val spatialCell: String = "spatial_cell"
    override val dayTimeDuration: String = "day_time_duration"
    override val intervalBin: String = "interval_bin"
  }

  /**
    *
    * @param field
    * @param func
    * @param sqlExpr
    * @param newvar
    * @return DataType: DataType of the aggregated result
    *         String: AQL aggregate function string
    */
  def translateAggrFunc(field: Field,
                        func: AggregateFunc,
                        groupSource: String,
                        sqlExpr: String,
                        newvar: String
                       ): (DataType.DataType, String) = {
    def aggFuncExpr(aggFunc: String): String = {
      if (field.name.equals("*")) {
        s"$aggFunc($groupSource) as `$newvar`"
      } else {
        s"$aggFunc( (select value $groupSource.$sqlExpr from $groupSource) ) as `$newvar`"
      }
    }
    func match {
      case Count =>
        if (field.dataType != DataType.Record) throw new QueryParsingException("count requires to aggregate on the record bag")
        (DataType.Number, aggFuncExpr(functionNames.count))
      case Max =>
        if (field.dataType != DataType.Number) throw new QueryParsingException("Max requires to aggregate on numbers")
        (DataType.Number, aggFuncExpr(functionNames.max))
      case Min =>
        if (field.dataType != DataType.Number) throw new QueryParsingException("Min requires to aggregate on numbers")
        (DataType.Number, aggFuncExpr(functionNames.min))
      case topK: TopK => ???
      case Avg =>
        if (field.dataType != DataType.Number) throw new QueryParsingException("Avg requires to aggregate on numbers")
        (DataType.Number, aggFuncExpr(functionNames.avg))
      case Sum =>
        if (field.dataType != DataType.Number) throw new QueryParsingException("Sum requires to aggregate on numbers")
        (DataType.Number, aggFuncExpr(functionNames.sum))
      case DistinctCount => ???
    }
  }

  //sqlpp is different from aql on 'in'
  protected def translateNumberRelation(field: Field,
                                        funcOpt: Option[TransformFunc],
                                        sqlExpr: String,
                                        relation: Relation,
                                        values: Seq[AnyVal]): String = {
    relation match {
      case Relation.inRange =>
        if (values.size != 2) throw new QueryParsingException(s"relation: $relation require two parameters")
        s"$sqlExpr >= ${values(0)} and $sqlExpr < ${values(1)}"
      case Relation.in =>
        s"$sqlExpr in [ ${values.mkString(",")} ]"
      case _ =>
        if (values.size != 1) throw new QueryParsingException(s"relation: $relation require one parameter")
        s"$sqlExpr $relation ${values.head}"
    }
  }

}