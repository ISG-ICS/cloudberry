package edu.uci.ics.cloudberry.zion.asterix

import akka.testkit.{TestActor, TestProbe}
import edu.uci.ics.cloudberry.zion.actor.{MockConnClient, TestkitExample}
import org.specs2.mutable.SpecificationLike

class TwitterKeywordViewActorTest extends TestkitExample with SpecificationLike with MockConnClient with TestData {

  "TwitterKeywordViewActorTest" should {
    "createSourceQuery" in {
      ok
    }

    "askViewOnly" in {
      ok
    }

  }
}
