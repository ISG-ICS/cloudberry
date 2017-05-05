package controllers

import java.io.File

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.{ActorMaterializer, Materializer}
import org.specs2.mutable.SpecificationLike
import play.api.libs.json._

import scala.concurrent.Await
import scala.concurrent.duration._

class CloudberryTest extends SpecificationLike {

  "application" should {

    "generate a actor flow" in {

      implicit val system : ActorSystem = ActorSystem("test")
      implicit val mat: Materializer = ActorMaterializer()
      def props(outActor: ActorRef) = Props(new Actor {
        override def receive = {
          case _ =>
            (1 to 10).foreach(outActor ! _)
            outActor ! done
        }
      })

      val flow = Cloudberry.actorFlow(props, done)
      val future = Source.single(42).via(flow).runWith(Sink.fold[Int, Int](0)(_ + _))
      val result = Await.result(future, 3.seconds)
      result must_== (1 to 10).sum
    }
  }
}
