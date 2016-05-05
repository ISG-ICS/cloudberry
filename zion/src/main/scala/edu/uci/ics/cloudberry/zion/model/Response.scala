package edu.uci.ics.cloudberry.zion.model

trait Response {

}

import play.api.libs.json.{Format, Json}

object SpatialTimeCount {
  implicit val keyCountFormatter: Format[KeyCountPair] = Json.format[KeyCountPair]
  implicit val countFormatter: Format[SpatialTimeCount] = Json.format[SpatialTimeCount]
}

case class SpatialTimeCount(map: Seq[KeyCountPair], time: Seq[KeyCountPair], hashtag: Seq[KeyCountPair]) extends Response
