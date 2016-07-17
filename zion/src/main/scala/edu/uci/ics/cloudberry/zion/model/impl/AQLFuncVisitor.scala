package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.QueryParsingException
import edu.uci.ics.cloudberry.zion.model.schema.Relation.Relation
import edu.uci.ics.cloudberry.zion.model.schema._

object AQLFuncVisitor {
  def translateRelation(field: Field,
                        funcOpt: Option[TransformFunc],
                        sourceVar: String,
                        relation: Relation,
                        values: Seq[AnyVal]
                       ): String = ???

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
          s"""
             |get-interval-start(interval-bin($$$sourceVar.${field.name}, datetime("1990-01-01T00:00:00.000Z"), $duration),
           """.stripMargin
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
