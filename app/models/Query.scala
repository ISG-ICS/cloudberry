package models

import com.esri.core.geometry.Polygon
import org.joda.time.Interval

trait Predicate {}

// Only working on AND relation so far
case class KeywordPredicate(val keywords: Seq[String]) extends Predicate

// Only working on Intersection relation so far
case class SpatialPredicate(val area: Polygon) extends Predicate

// Only working on Contains relation so far
case class TimeIntervalPredicate(val timeInterval: Interval) extends Predicate

case class AggregateQuery(val aggrFunction: AggregationFunction, val fields: Seq[String])

case class Query(keywordPredicate: KeywordPredicate,
                 spatialPredicate: SpatialPredicate,
                 timeIntervalPredicate: TimeIntervalPredicate,
                 groupLevel: Int,
                 aggregateQuery: AggregateQuery)


case class QueryResult(groupLevel: Int, map: Map[String, Number]) {
  def +(r2: QueryResult): QueryResult = ???
}

