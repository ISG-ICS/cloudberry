package edu.uci.ics.cloudberry.zion.actor

import java.util.concurrent.Executors

import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.actor.TestkitExample
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.impl.TwitterDataStore
import edu.uci.ics.cloudberry.zion.model.schema.{AppendView, Query}
import edu.uci.ics.cloudberry.zion.model.util.MockConnClient
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.specs2.mutable.SpecificationLike
import play.api.libs.json.{JsNumber, JsObject}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class DataSetAgentTest extends TestkitExample with SpecificationLike with MockConnClient {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  sequential

  val schema = TwitterDataStore.TwitterSchema

  "DataSetAgent" should {
    "answer query" in {
      val sender = new TestProbe(system)
      val mockQueryParser = mock[IQLGenerator]
      val mockConn = mock[IDataConn]

      val aqlString = ""
      val jsResponse = JsObject(Seq("a" -> JsNumber(1)))
      val query = Query("twitter")
      when(mockQueryParser.generate(query, schema)).thenReturn(aqlString)
      when(mockConn.postQuery(aqlString)).thenReturn(Future(jsResponse))

      val agent = system.actorOf(DataSetAgent.props(schema, mockQueryParser, mockConn))
      sender.send(agent, query)
      sender.expectMsg(jsResponse)
      ok
    }
    "queue the a serials append query" in {

      val sender1 = new TestProbe(system)
      val sender2 = new TestProbe(system)
      val mockQueryParser = mock[IQLGenerator]
      val mockConn = mock[IDataConn]

      val aqlString = "1"
      val append = AppendView("twitter", Query("twitter"))
      when(mockQueryParser.generate(append, schema)).thenReturn(aqlString)

      when(mockConn.postControl(aqlString)).thenAnswer(new Answer[Future[Boolean]] {
        override def answer(invocation: InvocationOnMock): Future[Boolean] = {
          Thread.sleep(1000)
          Future(true)
        }
      })

      val agent = system.actorOf(DataSetAgent.props(schema, mockQueryParser, mockConn))

      sender1.send(agent, append)
      sender2.send(agent, append)

      sender1.expectNoMsg(900 milli)
      sender1.expectMsg(1000 milli, true)
      sender2.expectNoMsg(900 milli)
      sender2.expectMsg(true)
      ok
    }
  }
}
