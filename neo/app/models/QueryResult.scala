package models

import edu.uci.ics.cloudberry.zion.model.KeyCountPair
import play.api.libs.json.{Format, Json}

object QueryResult {
  val Empty = QueryResult("", Seq.empty[KeyCountPair])
  implicit val format: Format[QueryResult] = Json.format[QueryResult]
}

case class QueryResult(aggType: String, result: Seq[KeyCountPair])