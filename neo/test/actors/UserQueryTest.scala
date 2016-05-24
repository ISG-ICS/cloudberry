package actors

import models.UserQuery
import org.specs2.mutable.Specification
import play.api.libs.json.{JsNull, JsNumber, JsObject, JsString}

class UserQueryTest extends Specification {

  "UserQuery" should {
    "return None if the json didn't have it" in {
      val jsonQuery = JsObject(Seq(
        "dataset" -> JsString("twitter"),
        "keyword" -> JsNull,
        "area" -> JsObject(Seq("swLog" -> JsNumber(-46.23046874999999),
                               "swLat" -> JsNumber(53.85252660044951),
                               "neLog" -> JsNumber(-146.42578125),
                               "neLat" -> JsNumber(21.453068633086783))),
        "timeRange" -> JsObject(Seq(
          "start" -> JsNumber(0),
          "end" -> JsNumber(2000))),
        "level" -> JsString("state"),
        "repeatDuration" -> JsNumber(0)))
      val userQuery = jsonQuery.as[UserQuery]
      userQuery.keyword must_== None
    }
  }
}
