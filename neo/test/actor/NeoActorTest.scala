package actor

import play.api.Logger

// TODO import from different module for testkitExample

import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestProbe
import akka.stream.scaladsl.Source
import akka.util.ByteString
import edu.uci.ics.cloudberry.zion.common.Config
import models.{GeoLevel, TimeBin, UserRequest}
import NeoActor.RequestType._
import org.joda.time.{DateTimeZone, Interval}
import org.specs2.mutable.SpecificationLike
import org.specs2.mock.Mockito
import org.mockito.Mockito._
import play.api.libs.json._
import play.api.libs.ws._

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future}

class NeoActorTest extends TestkitExampleForNeo with SpecificationLike with Mockito{

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
  implicit val materializer: Materializer = ActorMaterializer()

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

    "handle HTTP request/response to Berry" in {
      val url = "localhost:9000"
      val userRequest = UserRequest(
        "twitter.ds_tweet",
        Seq("zika"),
        Interval.parse("2015-11-22T00:00:00.000-08:00/2017-02-17T20:05:41.000-08:00"),
        TimeBin.Day,
        GeoLevel.State,
        Seq(37, 51)
      )
      val berryRequest = NeoActor.generateCBerryRequest(userRequest)

      val json1 = Json.obj("city" -> "NY", "count" -> 100)
      //val json2 = Json.obj("city" -> "LA", "count" -> 150)

      val ws = mock[WSClient]
      val wsRequest = mock[WSRequest]
      val header = DefaultWSResponseHeaders(200, Map.empty)
      val source: Source[ByteString, _] =
        Source(List(ByteString(json1.toString())))

      when(ws.url(any)).thenReturn(wsRequest)
      when(wsRequest.withMethod(any)).thenReturn(wsRequest)
      when(wsRequest.withBody(berryRequest(Sample))).thenReturn(wsRequest)
      when(wsRequest.stream()).thenReturn(Future(StreamedResponse(header, source)))

      val frontEnd = new TestProbe(system)
      val neo = system.actorOf(NeoActor.props(frontEnd.ref, ws, url, Config.Default))

      frontEnd.send(neo, userRequest)
      frontEnd.expectMsg()
      ok
    }
  }
}
