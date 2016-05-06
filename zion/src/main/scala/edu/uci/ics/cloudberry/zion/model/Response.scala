package edu.uci.ics.cloudberry.zion.model

import play.api.libs.json.{Format, Json}

trait Response {

}

case class SpatialTimeCount(map: Seq[KeyCountPair], time: Seq[KeyCountPair], hashtag: Seq[KeyCountPair]) extends Response

object SpatialTimeCount {
  implicit val keyCountFormatter: Format[KeyCountPair] = Json.format[KeyCountPair]
  implicit val countFormatter: Format[SpatialTimeCount] = Json.format[SpatialTimeCount]
}

