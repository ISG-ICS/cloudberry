package edu.uci.ics.cloudberry.zion.actor

import java.util.concurrent.Executors

import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.impl.TwitterDataStore
import edu.uci.ics.cloudberry.zion.model.schema.{AppendView, Query}
import edu.uci.ics.cloudberry.zion.model.util.MockConnClient
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.specs2.mutable.SpecificationLike
import play.api.libs.json.{JsNumber, JsObject}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

class ViewDataAgentTest extends TestkitExample with SpecificationLike with MockConnClient {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  val schema = TwitterDataStore.TwitterSchema

  sequential

  "ViewDataAgent" should {
    "answer query" in {
      val sender = new TestProbe(system)
      val mockQueryParser = mock[IQLGenerator]
      val mockConn = mock[IDataConn]

      val dbName = "test"
      val aqlString = ""
      val jsResponse = JsObject(Seq("a" -> JsNumber(1)))
      val query = Query(dbName)
      when(mockQueryParser.generate(query, Map(dbName -> schema))).thenReturn(aqlString)
      when(mockConn.postQuery(aqlString)).thenReturn(Future(jsResponse))

      val agent = system.actorOf(ViewDataAgent.props(dbName, schema, mockQueryParser, mockConn, Config.Default))
      sender.send(agent, query)
      sender.expectMsg(jsResponse)
      ok
    }
    "queue the a serials of append queries" in {

      val sender1 = new TestProbe(system)
      val sender2 = new TestProbe(system)
      val mockQueryParser = mock[IQLGenerator]
      val mockConn = mock[IDataConn]

      val dbName = "test"
      val aqlString = "1"
      val append = AppendView("twitter", Query(dbName))
      when(mockQueryParser.generate(append, Map(dbName -> schema))).thenReturn(aqlString)

      when(mockConn.postControl(aqlString)).thenAnswer(new Answer[Future[Boolean]] {
        override def answer(invocation: InvocationOnMock): Future[Boolean] = {
          Thread.sleep(1000)
          Future(true)
        }
      })

      val agent = system.actorOf(ViewDataAgent.props("view", schema, mockQueryParser, mockConn, Config.Default))

      sender1.send(agent, append)
      sender2.send(agent, append)

      sender1.expectNoMsg(900 milli)
      sender1.expectMsg(2000 milli, true)
      sender2.expectNoMsg(900 milli)
      sender2.expectMsg(true)
      ok
    }
  }
}
