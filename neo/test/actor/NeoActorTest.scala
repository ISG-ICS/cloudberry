package actor

import models.{GeoLevel, TimeBin, UserRequest}
import org.joda.time.{DateTimeZone, Interval}
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class NeoActorTest extends Specification {

  "NeoActor" should {
    "generateCBerryRequest" in {
      import NeoActor._
      import NeoActor.RequestType._

      DateTimeZone.setDefault(DateTimeZone.UTC)
      val userRequest = UserRequest("twitter", Seq("zika", "virus"), new Interval(0, 2000), TimeBin.Hour, GeoLevel.City, Seq(1, 2, 3, 4))
      val cbRequest = generateCBerryRequest(userRequest)
      cbRequest(ByPlace) must_== Json.parse(
        """
          |{ "dataset":"twitter",
          |  "filter":[
          |    {"field":"geo_tag.cityID","relation":"in","values":[1,2,3,4]},
          |    {"field":"create_at","relation":"inRange","values":["1970-01-01T00:00:00.000Z","1970-01-01T00:00:02.000Z"]},
          |    {"field":"text","relation":"contains","values":["zika","virus"]}],
          |  "group":{
          |    "by":[
          |      { "field":"geo",
          |        "apply":{"name":"level","args":{"level":"city"}},
          |        "as":"city"
          |      }
          |    ],
          |    "aggregate":[
          |      { "field":"*",
          |        "apply":{"name":"count"},
          |        "as":"count"
          |      }
          |    ]
          |  }
          |}
        """.stripMargin)
      cbRequest(ByTime) must_== Json.parse(
        """
          |{ "dataset":"twitter",
          |  "filter":[
          |    {"field":"geo_tag.cityID","relation":"in","values":[1,2,3,4]},
          |    {"field":"create_at","relation":"inRange","values":["1970-01-01T00:00:00.000Z","1970-01-01T00:00:02.000Z"]},
          |    {"field":"text","relation":"contains","values":["zika","virus"]}
          |  ],
          |  "group":{
          |    "by":[
          |      { "field":"create_at",
          |        "apply":{"name":"interval","args":{"unit":"hour"}},
          |        "as":"hour"}
          |    ],
          |    "aggregate":[
          |      { "field":"*",
          |        "apply":{"name":"count"},
          |        "as":"count"
          |      }
          |    ]
          |  }
          |}
        """.stripMargin)
      cbRequest(ByHashTag) must_== Json.parse(
        """
          |{ "dataset":"twitter",
          |  "filter":[
          |    {"field":"geo_tag.cityID","relation":"in","values":[1,2,3,4]},
          |    {"field":"create_at","relation":"inRange","values":["1970-01-01T00:00:00.000Z","1970-01-01T00:00:02.000Z"]},
          |    {"field":"text","relation":"contains","values":["zika","virus"]}
          |  ],
          |  "unnest":[{"hashtags":"tag"}],
          |  "group":{
          |    "by":[{"field":"tag"}],
          |    "aggregate":[{"field":"*","apply":{"name":"count"},"as":"count"}]
          |  },
          |  "select":{
          |    "order":["-count"],
          |    "limit":50,
          |    "offset":0
          |  }
          |}
        """.stripMargin)
      cbRequest(Sample) must_== Json.parse(
        """
          |{ "dataset":"twitter",
          |  "filter":[
          |    {"field":"geo_tag.cityID","relation":"in","values":[1,2,3,4]},
          |    {"field":"create_at","relation":"inRange","values":["1970-01-01T00:00:00.000Z","1970-01-01T00:00:02.000Z"]},
          |    {"field":"text","relation":"contains","values":["zika","virus"]}
          |  ],
          |  "select":{
          |    "order":["-create_at"],
          |    "limit":10,
          |    "offset":0,
          |    "field":["create_at","id","user.id"]
          |  }
          |}
        """.stripMargin)
      ok
    }
  }
}
