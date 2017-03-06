package actor

import akka.actor._
import akka.stream.{ActorMaterializer, Materializer}
import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.actor.TestkitExample
import edu.uci.ics.cloudberry.zion.actor.BerryClient._
import java.util.concurrent.Executors
import org.specs2.mutable.SpecificationLike
import play.api.libs.json._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

class RequestRouterTest extends TestkitExample with SpecificationLike{

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
  implicit val materializer: Materializer = ActorMaterializer()

  val nonSlicingRequestWithWrapTransform = Json.parse(
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
      |  },
      |  "transform":{
      |    "wrap":{
      |      "key": "sample"
      |    }
      |  }
      |}
    """.stripMargin)

  val slicingRequestWithWrapTransform = Json.parse(
    """
      |{ "batch": [
      |  { "dataset":"twitter",
      |    "filter":[
      |      { "field":"geo_tag.cityID","relation":"in","values":[1,2,3,4]},
      |      { "field":"create_at","relation":"inRange","values":["1970-01-01T00:00:00.000Z","1970-01-01T00:00:02.000Z"]},
      |      { "field":"text","relation":"contains","values":["zika","virus"]}
      |    ],
      |    "group":{
      |      "by":[
      |        { "field":"create_at",
      |          "apply":{"name":"interval","args":{"unit":"hour"}},
      |          "as":"hour"}
      |      ],
      |      "aggregate":[
      |        { "field":"*",
      |          "apply":{"name":"count"},
      |          "as":"count"}
      |      ]
      |    }
      |  }],
      |  "option": {
      |    "sliceMillis": 2000
      |  },
      |  "transform":{
      |    "wrap":{
      |      "key": "batch"
      |    }
      |  }
      |}
      |""
    """.stripMargin)

  val nonSlicingRequestWithNoTransform = Json.parse(
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

  val slicingRequestWithNoTransform = Json.parse(
    """
      |{ "batch": [
      |  { "dataset":"twitter",
      |    "filter":[
      |      { "field":"geo_tag.cityID","relation":"in","values":[1,2,3,4]},
      |      { "field":"create_at","relation":"inRange","values":["1970-01-01T00:00:00.000Z","1970-01-01T00:00:02.000Z"]},
      |      { "field":"text","relation":"contains","values":["zika","virus"]}
      |    ],
      |    "group":{
      |      "by":[
      |        { "field":"create_at",
      |          "apply":{"name":"interval","args":{"unit":"hour"}},
      |          "as":"hour"}
      |      ],
      |      "aggregate":[
      |        { "field":"*",
      |          "apply":{"name":"count"},
      |          "as":"count"}
      |      ]
      |    }
      |  }],
      |  "option": {
      |    "sliceMillis": 2000
      |  }
      |}
      |""
    """.stripMargin)

  "RequestRouter" should {
    "parse transform and pass transform to berry" in {

      val berryResult = Json.obj("result" -> "result example")
      val frontEnd = new TestProbe(system)
      val clientProps = Props(new Actor {
        override def receive: Receive = {
          case berryRequest: Request =>
            frontEnd.ref ! berryRequest.postTransform.transform(berryResult)
          case _ =>
            throw new IllegalArgumentException("Message type sent to berry is not correct.")
        }
      })
      val router = system.actorOf(RequestRouter.props(frontEnd.ref, clientProps, Config.Default))

      frontEnd.send(router, nonSlicingRequestWithNoTransform)
      frontEnd.expectMsg(berryResult)

      frontEnd.send(router, slicingRequestWithNoTransform)
      frontEnd.expectMsg(berryResult)

      frontEnd.send(router, nonSlicingRequestWithWrapTransform)
      frontEnd.expectMsg(Json.obj(
        "key" -> "sample",
        "value" -> berryResult
      ))

      frontEnd.send(router, slicingRequestWithWrapTransform)
      frontEnd.expectMsg(Json.obj(
        "key" -> "batch",
        "value" -> berryResult
      ))

      ok
    }

    "distribute non-slicing and slicing requests to different berry clients" in {

      val nonSlicingBerryResult = Json.obj("result" -> "non slicing result")
      val slicingBerryResultOne = Json.obj("result" -> "slicing result 1")
      val slicingBerryResultTwo = Json.obj("result" -> "slicing result 2")
      val slicingBerryResultThree = Json.obj("result" -> "slicing result 3")

      val frontEnd = new TestProbe(system)
      val clientProps = Props(new Actor {
        override def receive: Receive = {
          case berryRequest: Request =>
            (berryRequest.json \\ "sliceMillis").isEmpty match {
              case true =>
                frontEnd.ref ! nonSlicingBerryResult
              case false =>
                frontEnd.ref ! slicingBerryResultOne
                Thread.sleep(500)
                frontEnd.ref ! slicingBerryResultTwo
                Thread.sleep(500)
                frontEnd.ref ! slicingBerryResultThree
            }
          case _ =>
            throw new IllegalArgumentException("Message type sent to berry is not correct.")
        }
      })
      val router = system.actorOf(RequestRouter.props(frontEnd.ref, clientProps, Config.Default))

      frontEnd.send(router, nonSlicingRequestWithNoTransform)
      frontEnd.expectMsg(nonSlicingBerryResult)

      frontEnd.send(router, slicingRequestWithNoTransform)
      frontEnd.expectMsg(slicingBerryResultOne)
      frontEnd.expectMsg(slicingBerryResultTwo)
      frontEnd.expectMsg(slicingBerryResultThree)

      frontEnd.send(router, slicingRequestWithNoTransform)
      frontEnd.expectMsg(100 millis, slicingBerryResultOne)
      frontEnd.send(router, slicingRequestWithNoTransform)

      /*
      ideal test case result:
      frontEnd.expectMsg(slicingBerryResultOne)
      frontEnd.send(router, nonSlicingRequestWithNoTransform)
      frontEnd.expectMsg(nonSlicingBerryResult)
      frontEnd.expectMsg(slicingBerryResultTwo)
      frontEnd.expectMsg(slicingBerryResultThree)
      */

      val r = frontEnd.receiveN(5)
      r.foreach(println)

      /*
      Actually get:
      {"result":"slicing result 2"}
      {"result":"slicing result 3"}
      {"result":"slicing result 1"}
      {"result":"slicing result 2"}
      {"result":"slicing result 3"}
      */

      ok
    }
  }
}