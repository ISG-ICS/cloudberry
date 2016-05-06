package edu.uci.ics.cloudberry.zion.model

import org.specs2.mutable.Specification
import play.api.libs.json.{JsArray, Json}

class SpatialTimeCountTest extends Specification {

  "SpatialTimeCountTest" should {
    "countFormatter" in {
      val string =
        """[{"map":[{"key":1,"count":1},{"key":55,"count":2}],
          |"time":[{"key":"2012-05-01","count":1},{"key":"2012-12-10","count":1},{"key":"2012-12-11","count":1},{"key":"2012-12-19","count":5}],
          |"hashtag":[{"key":"newyork","count":2},{"key":"nyc","count":2},{"key":"trumptower","count":2},{"key":"4More4PresObama","count":1},{"key":"teaparty","count":1}]}]
          |""".stripMargin
      val json = Json.parse(string)
      val obj = json.asInstanceOf[JsArray].apply(0)
      print(obj.as[SpatialTimeCount])
      ok
    }

  }
}
