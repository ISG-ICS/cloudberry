package models

import com.esri.core.geometry.Polygon
import org.joda.time.{DateTime, Interval}
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


case class QueryResult(level: Int, aggResult: Map[String, Int]) {
  def +(r2: QueryResult): QueryResult = {
    QueryResult(level, aggResult ++ r2.aggResult)
  }

  def +(r2: Option[QueryResult]): QueryResult = {
    r2 match {
      case Some(r) => this + r
      case None => this
    }
  }
}

object QueryResult {
  val Empty = QueryResult(1, Map())
  val SampleCache = QueryResult(1, Map("CA" -> 1340, "NV" -> 560))
  val SampleView = QueryResult(1, Map("AZ" -> 2))
  val Failure = QueryResult(-1, Map("Null" -> 0))

  implicit val mapFormatter: Format[Map[String, Int]] = {
    new Format[Map[String, Int]] {
      override def writes(m: Map[String, Int]): JsValue = {
        val fields: Seq[(String, JsValue)] = m.map {
          case (k, v) => k -> JsNumber(v.intValue())
        }(collection.breakOut)
        JsObject(fields)
      }

      override def reads(json: JsValue): JsResult[Map[String, Int]] = {
        JsSuccess {
          json.asOpt[JsArray] match {
            case Some(array) => {
              val builder = Map.newBuilder[String, Int]
              array.value.foreach { pair =>
                pair.asOpt[JsObject] match {
                  case Some(obj) => {
                    val each = obj.fields.toMap.mapValues { v =>
                      v.asOpt[JsNumber] match {
                        case Some(number) => number.value.toInt
                        case None => -1
                      }
                    }
                    each.foreach { e => builder += e._1 -> e._2 }
                  }
                  case other =>
                }
              }
              builder.result()
            }
            case None => Map.empty[String, Int]
          }
        }
      }

    }
  }
  implicit val writer = Json.format[QueryResult]
}

case class ViewMetaRecord(dataSetName: String, keyword:String, timeStart: DateTime, timeEnd: DateTime)

object ViewMetaRecord {

}
