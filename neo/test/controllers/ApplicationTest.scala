package controllers

import java.io.File

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.specs2.mock._
import org.specs2.mutable.SpecificationLike
import play.api.{Configuration, Environment}
import play.api.libs.json._
import play.api.mvc._
import play.api.test._
import play.api.libs.ws._
import play.api.test.Helpers._

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class ApplicationTest extends SpecificationLike with Mockito {

  implicit val system : ActorSystem = ActorSystem("test")
  implicit val mat: Materializer = ActorMaterializer()

  "application" should {

    val cities = Application.loadCity(new File("test/resources/data/city.sample.json"))

    "load the city data from a file" in {
      cities.size must_== 1006
    }

    "calculate centroid" in {
      math.abs((cities.apply(0) \ "centroidLongitude").as[Double] - (-176.6287565)) must be <= 0.00001
      math.abs((cities.apply(0) \ "centroidLatitude").as[Double] - 51.8209920) must be <= 0.00001
    }

    "find cities whose centroids are in the current region" in {
      val result = Application.findCity(35, 33, -85, -87, cities)
      val features = (result \ "features").as[JsArray]
      val cityIDs = List.newBuilder[Double]
      for (city <- features.value) {
        cityIDs += (city \ "properties" \ "cityID").as[Double]
      }
      cityIDs.result().contains(100820) must_== true
    }

    "generate a actor flow" in {

      def props(outActor: ActorRef) = Props(new Actor {
        override def receive = {
          case _ =>
            (1 to 10).foreach(outActor ! _)
            outActor ! done
        }
      })

      val flow = Application.actorFlow(props, done)
      val future = Source.single(42).via(flow).runWith(Sink.fold[Int, Int](0)(_ + _))
      val result = Await.result(future, 3.seconds)
      result must_== (1 to 10).sum
    }

    "register new schema" in {

      val dataSetField1 = new Field("field1", DataType.String, false)
      val dataSetField2 = new Field("field2", DataType.Number, false)
      val registerRequest = Json.parse(
        s"""
           |{
           |  "dataset": "test",
           |  "schema" : {
           |    "typeName"    : "newType",
           |    "dimension"   : [${dataSetField1}, ${dataSetField2}],
           |    "measurement" : [${dataSetField1}, ${dataSetField2}],
           |    "primaryKey"  : ["key1", "key2"],
           |    "timeField"   : "03/14/2017"
           |  }
           |}
       """.stripMargin
      )


      // Need to reconsider test logic.
      // instantiate Application Class is not a good choice:
      // 1. some dependencies cannot find, i.e. city.sample.json
      // 2. it links to a real data manager and adds test tables to database

      // Alternatives:
      // 1. Repick EssentialAction
      // 2. define a function in object and call the function in Action and then test the function in object instead, like function actorFlow in Action BerryQuery

      /*
      val wsClient = mock[WSClient]
      val configuration = mock[Configuration]
      val environment = mock[Environment]

      val application = new Application(wsClient, configuration, environment)
      val result : Future[Result] = application.register.apply(FakeRequest(POST, "/").withJsonBody(registerRequest)).run()

      status(result) mustEqual OK
      contentAsString(result) mustEqual "Dataset test has been registered."
      */

      ok
    }
  }
}
