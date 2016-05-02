package models

import com.vividsolutions.jts.geom.{Envelope, Polygon}
import db.AQL
import org.joda.time.{DateTime, Interval}
import play.api.libs.json._

trait Predicate {
  def toAQLString(variable: String, fieldName: String): String
}

object NoPredicate extends Predicate {
  override def toAQLString(variable: String, fieldName: String): String = ""
}

// Only working on AND relation so far
case class KeywordPredicate(val keywords: Seq[String]) extends Predicate {
  override def toAQLString(variable: String, fieldName: String): String =
    keywords.map(
      keyword =>
        s"""
           |where similarity-jaccard(word-tokens($${$variable}."${fieldName}"), word-tokens("${keyword}")) > 0.0
           |""".stripMargin
    ).mkString("\n")
}

// Only working on Contains relation so far
case class TimeIntervalPredicate(val interval: Interval) extends Predicate {

  import AQL._

  override def toAQLString(variable: String, fieldName: String): String =
    s"""
       |where $${$variable}."${fieldName}">= datetime("${TimeFormat.print(interval.getStart)}")
       |and $${$variable}."${fieldName}" < datetime("${TimeFormat.print(interval.getEnd)}")
     """.stripMargin
}

case class AggregateQuery(val aggrFunction: AggregationFunction, val fields: Seq[String])

case class Query(keywordPredicate: KeywordPredicate,
                 timeIntervalPredicate: TimeIntervalPredicate,
                 groupLevel: Int,
                 aggregateQuery: AggregateQuery)


case class QueryResult(aggType: String, dataset: String, keyword: String, result: Map[String, Int]) {
  def +(r2: QueryResult): QueryResult = {
    if (this == QueryResult.Empty) {
      return r2
    } else if (r2 == QueryResult.Empty) {
      return this
    }
    if (this.aggType != r2.aggType || this.dataset != r2.dataset || this.keyword != r2.keyword) {
      throw new IllegalArgumentException
    }
    this.copy(result = result ++ r2.result)
  }

  def +(r2: Option[QueryResult]): QueryResult = {
    r2 match {
      case Some(r) => this + r
      case None => this
    }
  }
}

object QueryResult {
  val Empty = QueryResult("null", "null", "null", Map())
  val SampleCache = QueryResult("map", DataSet.Twitter.name, "rain", Map("CA" -> 1340, "NV" -> 560))
  val SampleView = SampleCache.copy(result = Map("AZ" -> 2))
  val Failure = Empty

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
  implicit val formatter = Json.format[QueryResult]
}

case class ViewMetaRecord(dataset: String, keyword: String, timeStart: DateTime, timeEnd: DateTime) {
  val interval = new Interval(timeStart, timeEnd)
}

object ViewMetaRecord {
  val timeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
  implicit val jodaTimeReaders = Reads.jodaDateReads(timeFormat)
  implicit val jodaTimeWriters = Writes.jodaDateWrites(timeFormat)
  implicit val formatter = Json.format[ViewMetaRecord]
}
