package edu.uci.ics.cloudberry.zion.actor

import java.util.concurrent.Executors

import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.actor.TestkitExample
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

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class BaseDataAgentTest extends TestkitExample with SpecificationLike with MockConnClient {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  sequential

  val schema = TwitterDataStore.TwitterSchema

  "BaseDataSetAgent" should {
    "answer query" in {
      val sender = new TestProbe(system)
      val mockQueryParser = mock[IQLGenerator]
      val mockConn = mock[IDataConn]

      val aqlString = ""
      val jsResponse = JsObject(Seq("a" -> JsNumber(1)))
      val query = Query("twitter")
      when(mockQueryParser.generate(query, schema)).thenReturn(aqlString)
      when(mockConn.postQuery(aqlString)).thenReturn(Future(jsResponse))

      val agent = system.actorOf(BaseDataAgent.props("test", schema, mockQueryParser, mockConn, Config.Default))
      sender.send(agent, query)
      sender.expectMsg(jsResponse)
      ok
    }
    "answer estimable query" in {
      ok
    }
    "collect stats information" in {
      ok
    }

  }
}
