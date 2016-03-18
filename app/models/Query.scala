package models

import com.esri.core.geometry.Polygon
import org.joda.time.Interval
import play.api.libs.json._

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


case class QueryResult(level: Int, aggResult: Map[String, Number]) {
  def +(r2: QueryResult): QueryResult = ???
}

object QueryResult {
  val Sample = QueryResult(1, Map("CA" -> 1340, "NV" -> 560, "AZ" -> 2))

  implicit val mapFormatter: Format[Map[String, Number]] = {
    new Format[Map[String, Number]] {
      override def writes(m: Map[String, Number]): JsValue = {
        val fields: Seq[(String, JsValue)] = m.map {
          case (k, v) => k -> JsNumber(v.doubleValue())
        }(collection.breakOut)
        JsObject(fields)
      }

      override def reads(json: JsValue): JsResult[Map[String, Number]] = {
        json.validate[Map[String, Number]].map(_.map {
          case (key, value) => key -> value
        })
      }

    }
  }
  implicit val writer = Json.format[QueryResult]
}


