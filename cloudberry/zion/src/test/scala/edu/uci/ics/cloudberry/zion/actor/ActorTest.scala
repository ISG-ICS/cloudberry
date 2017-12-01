package edu.uci.ics.cloudberry.zion.actor

import java.util.concurrent.Executors

import org.specs2.mutable.SpecificationLike

import scala.concurrent.ExecutionContext

class ActorTest extends TestkitExample with SpecificationLike {

  import akka.actor.{Actor, PoisonPill, Props}

  case object Ping

  case object Pong

  class Pinger extends Actor {
    var countDown = 100

    val ponger = context.actorOf(Props(new Ponger()))

    override def preStart(): Unit = {
      System.err.println("Pinger started")
    }

    override def postStop(): Unit = {
      System.err.println("Pinger has stopped")
      super.postStop()
    }

    def receive = {
      case Pong =>
        println(s"${self.path} received pong, count down $countDown")

        if (countDown > 0) {
          countDown -= 1
          ponger ! Ping
        } else {
          self ! PoisonPill
        }
    }
  }

  class Ponger extends Actor {

    override def preStart(): Unit = {
      System.err.println("Ponger started")
    }

    def receive = {
      case Ping =>
        println(s"${self.path} received ping")
        context.parent ! Pong
    }

    override def postStop(): Unit = {
      System.err.println("Ponger has stopped")
      super.postStop()
    }
  }

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))
  "Child" should {
    "close if parent is done" in {

      val pinger = system.actorOf(Props(new Pinger()), "pinger")
      pinger ! PoisonPill
      ok
    }
  }
}
