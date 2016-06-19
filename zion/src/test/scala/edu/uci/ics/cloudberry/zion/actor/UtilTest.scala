package edu.uci.ics.cloudberry.zion.actor

import edu.uci.ics.cloudberry.zion.asterix.TestData
import org.specs2.mock.Mockito
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.specs2.mutable.Specification

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._

class UtilTest extends Specification with TestData with Mockito {

  "A future" should {
    "be registered to multiple callback functions if using map" in {
      val fint = Future {
        Thread.sleep(100)
        200
      }
      var v1 = 300
      val fMapped = fint.map { v =>
        v1 += v
        v
      }

      Await.result(fMapped, 400 millisecond) must_== 200
      v1 must_== (500)
    }
  }

  "A mock" should {
    "throws something wrong if given unregistered value" in {
      class Foo() {
        def foo(arg: String): String = ???
      }
      val mockFoo = mock[Foo]
      when(mockFoo.foo("foo")).thenReturn("foo").thenReturn("foo2")
      println(mockFoo.foo("unknown"))
      mockFoo.foo("foo") must_== "foo"
      mockFoo.foo("unknown") must_!= "foo"
      mockFoo.foo("foo") must_== "foo2"
      mockFoo.foo("foo") must_== "foo2"
    }
  }
}
