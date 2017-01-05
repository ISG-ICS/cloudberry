package controllers

import java.nio.file.Paths

import akka.NotUsed
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.{ActorMaterializer, IOResult, OverflowStrategy}
import akka.stream.scaladsl.{FileIO, Flow, Keep, Sink, Source}
import akka.util.ByteString
import org.specs2.mutable.Specification

import scala.concurrent.{Await, Future, Promise}

/**
  * Created by jianfeng on 1/2/17.
  */
class Stream extends Specification {

  implicit val system = ActorSystem("QuickStart")
  implicit val materializer = ActorMaterializer()
  import scala.concurrent.ExecutionContext.Implicits.global

  def lineSink(filename: String): Sink[String, Future[IOResult]] =
    Flow[String]
      .map(s => ByteString(s + "\n"))
      .toMat(FileIO.toFile(???))(Keep.right)

  "Stream test" should {
    "try source" in {
      val s2 = Source.maybe[Int]
      val source: Source[Int, NotUsed] = Source(1 to 10)
      val factorials = source.scan(1)((acc, next) => acc * next)
      //      factorials.runWith(FileIO.toFile(???))
      val sink = Sink.reduce[Int]((a, b) => a + b)
//      val xsink = lineSink("abc")
      val flow = Flow[Int].toMat(sink)(Keep.right)
      val g = factorials.toMat(flow)(Keep.right)
      g.run()
      val x = factorials.runWith(flow)
      x.foreach(println)
      ok
    }
    "x" in {
      val (a,b)= Source.queue(100, OverflowStrategy.dropHead)
      ok
    }
  }
}
