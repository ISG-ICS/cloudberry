package edu.uci.ics.cloudberry.zion.actor

import edu.uci.ics.cloudberry.zion.asterix.TestData
import org.specs2.mutable.Specification

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class UtilTest extends Specification with TestData {

  "A future" should {
    "be registered to multiple callback functions is using map" in {
      val fint = Future {
        Thread.sleep(100)
        200
      }
      var v1 = 300
      val fMapped = fint.map { v =>
        v1 += v
        v
      }

      Await.result(fMapped, 150 millisecond) must_== 200
      v1 must_==(500)
    }
  }
}
