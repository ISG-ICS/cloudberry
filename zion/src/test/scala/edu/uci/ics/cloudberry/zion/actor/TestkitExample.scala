package edu.uci.ics.cloudberry.zion.actor

import akka.actor._
import akka.testkit._
import org.specs2.specification.After

import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
  * This class provides any enclosed specs with an ActorSystem and an implicit sender.
  * An ActorSystem can be an expensive thing to set up, so we define a single system
  * that is used for all of the tests.
  */
abstract class TestkitExample extends TestKit(ActorSystem()) with After with ImplicitSender {

  /**
    * Runs after the example completes.
    */
  override def after {
    // Send a shutdown message to all actors.
    system.terminate()

    // Block the current thread until all the actors have received and processed
    // shutdown messages.  Using this method makes certain that all threads have been
    // terminated, which is especially important when running large test suites (otherwise
    // you may find yourself running out of threads unexpectedly)
    Await.result(system.whenTerminated, Duration.Inf)
  }
}
