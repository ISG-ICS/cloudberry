package actor

import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestProbe
import akka.stream.scaladsl.Source
import akka.util.ByteString
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.actor.TestkitExample
import java.util.concurrent.Executors
import models.{GeoLevel, TimeBin, UserRequest}
import NeoActor.RequestType._
import org.joda.time.{DateTimeZone, Interval}
import org.specs2.mutable.SpecificationLike
import org.specs2.mock.Mockito
import org.mockito.Mockito._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.http.Writeable

import scala.concurrent.{ExecutionContext, Future}

class NeoActorTest extends TestkitExample with SpecificationLike with Mockito{

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
  implicit val materializer: Materializer = ActorMaterializer()

  "NeoActor" should {
    "generateCBerryRequest" in {
      import NeoActor._

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
      val interval = Interval.parse("2015-11-22T00:00:00.000-08:00/2017-02-14T17:44:56.000-08:00")
      val jsonRequest = Json.parse(
        s"""
           |{
           |  "dataset"     : "twitter.ds_tweet",
           |  "keywords"    : ["zika"],
           |  "timeInterval": ${JsObject(Seq(("start", JsNumber(interval.getStartMillis)), ("end", JsNumber(interval.getEndMillis))))},
           |  "timeBin"     : "day",
           |  "geoLevel"    : "state",
           |  "geoIds"      : [37, 51],
           |  "mergeResult" : ${false}
           |}
       """.stripMargin
      )
      val json1 = Json.obj("city" -> "NY", "count" -> 100)
      val json2 = Json.obj("city" -> "LA", "count" -> 150)

      val ws = mock[WSClient]
      val wsRequest = mock[WSRequest]
      val header = DefaultWSResponseHeaders(200, Map.empty)
      val source: Source[ByteString, _] =
        Source(List(ByteString(json1.toString()), ByteString("\n"), ByteString(json2.toString())))

      when(ws.url(any)).thenReturn(wsRequest)
      when(wsRequest.withMethod("POST")).thenReturn(wsRequest)
      when(wsRequest.withBody[JsValue](any[JsValue])(any[Writeable[JsValue]])).thenReturn(wsRequest)
      when(wsRequest.stream()).thenReturn(Future(StreamedResponse(header, source)))

      val frontEnd = new TestProbe(system)
      val neo = system.actorOf(NeoActor.props(frontEnd.ref, ws, url, Config.Default))
      frontEnd.send(neo, jsonRequest)

      val ExpectedSampleJson1 = Json.obj("key" -> Sample, "value" -> json1)
      val ExpectedBatchJson1 = Json.obj("key" -> Batch, "value" -> json1)
      val ExpectedSampleJson2 = Json.obj("key" -> Sample, "value" -> json2)
      val ExpectedBatchJson2 = Json.obj("key" -> Batch, "value" -> json2)

      val ExpectedStreamingResult1 = frontEnd.receiveN(2)
      ExpectedStreamingResult1.contains(ExpectedSampleJson1) must_== true
      ExpectedStreamingResult1.contains(ExpectedBatchJson1) must_== true
      val ExpectedStreamingResult2 = frontEnd.receiveN(2)
      ExpectedStreamingResult2.contains(ExpectedSampleJson2) must_== true
      ExpectedStreamingResult2.contains(ExpectedBatchJson2) must_== true
      ok
    }
  }
}
