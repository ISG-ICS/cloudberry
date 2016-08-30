package edu.uci.ics.cloudberry.zion.actor

import java.util.concurrent.Executors

import org.mockito.Mockito._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class UtilTest extends Specification with Mockito {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

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

      Await.result(fMapped, 5000 millisecond) must_== 200
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
