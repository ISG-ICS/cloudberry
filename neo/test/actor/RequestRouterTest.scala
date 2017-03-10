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

import scala.concurrent.ExecutionContext

class RequestRouterTest extends TestkitExample with SpecificationLike{

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
  implicit val materializer: Materializer = ActorMaterializer()

  "RequestRouter" should {
    "forward request and differentiate streaming and non-streaming requests" in {

      val nonStreamingRequest = Json.parse(
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
      val streamingRequest = Json.parse(
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

      val frontEnd = new TestProbe(system)
      val clientProps = Props(new Actor {
        override def receive: Receive = {
          case request: Request =>
            frontEnd.ref ! Json.obj("sender" -> self.path.name)
          case _ =>
            throw new IllegalArgumentException("Message type sent to berry is not correct.")
        }
      })
      val router = system.actorOf(RequestRouter.props(clientProps, Config.Default))

      frontEnd.send(router, nonStreamingRequest)
      frontEnd.expectMsg(Json.obj(
        "sender" -> "nonStreamingClient"
      ))

      frontEnd.send(router, streamingRequest)
      frontEnd.expectMsg(Json.obj(
        "sender" -> "streamingClient"
      ))

      ok
    }
  }
}