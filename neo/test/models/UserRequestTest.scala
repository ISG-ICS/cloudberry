package models

import org.joda.time.Interval
import org.specs2.mutable.Specification
import play.api.libs.json._

class UserRequestTest extends Specification {


  "UserRequest" should {
    "be able to import from a json request" in {
      val json = Json.obj("dataset" -> JsString("twitter"),
                          "keywords" -> JsArray(Seq(JsString("zika"), JsString("virus"))),
                          "timeInterval" -> Json.obj("start" -> JsNumber(0), "end" -> JsNumber(200)),
                          "timeBin" -> "hour",
                          "geoLevel" -> "city",
                          "geoIds" -> JsArray(Seq(JsNumber(1), JsNumber(2), JsNumber(3), JsNumber(4))))
      val userRequest = UserRequest("twitter", Seq("zika", "virus"), new Interval(0, 200), TimeBin.Hour, GeoLevel.City, Seq(1, 2, 3, 4))
      json.as[UserRequest] must_== (userRequest)
    }
  }
}
