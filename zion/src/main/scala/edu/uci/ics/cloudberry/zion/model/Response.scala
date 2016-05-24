package edu.uci.ics.cloudberry.zion.model

import play.api.libs.json.{Format, JsValue, Json, Writes}

trait Response {

}

case class SpatialTimeCount(map: Seq[KeyCountPair],
                            time: Seq[KeyCountPair],
                            hashtag: Seq[KeyCountPair]) extends Response

object SpatialTimeCount {
  implicit val keyCountFormatter: Format[KeyCountPair] = Json.format[KeyCountPair]
  implicit val countFormatter: Format[SpatialTimeCount] = Json.format[SpatialTimeCount]
}

case class SampleTweet(uid: String, msg: String, tid: String)

case class SampleList(result: Seq[SampleTweet]) extends Response {
  val aggType = "sample"
}

object SampleList {
  implicit val sampleListWrites: Writes[SampleList] = new Writes[SampleList] {
    override def writes(o: SampleList): JsValue = {
      Json.obj("aggType" -> "sample", "result" -> Json.toJson(o.result))
    }
  }
}

object SampleTweet {
  implicit val sampleFormatter: Format[SampleTweet] = Json.format[SampleTweet]
}

