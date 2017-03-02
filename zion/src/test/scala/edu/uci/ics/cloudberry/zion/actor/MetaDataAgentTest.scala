package edu.uci.ics.cloudberry.zion.actor

import java.util.concurrent.Executors

import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.impl.DataSetInfo
import edu.uci.ics.cloudberry.zion.model.schema.{Query, UpsertRecord}
import edu.uci.ics.cloudberry.zion.model.util.MockConnClient
import org.mockito.stubbing.Answer
import org.specs2.mutable.SpecificationLike
import play.api.libs.json.{JsArray, JsNumber, JsObject, Json}
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.Mockito._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class MetaDataAgentTest extends TestkitExample with SpecificationLike with MockConnClient {
  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  sequential

  "MetaDataAgent" should {
    "queue the a serials of upsert queries" in {

      val sender1 = new TestProbe(system)
      val sender2 = new TestProbe(system)
      val mockQueryParser = mock[IQLGenerator]
      val mockConn = mock[IDataConn]
      val schema = DataSetInfo.MetaSchema

      val aqlString = "mock aql"
      val testDB = "meta"
      val upsert = UpsertRecord(testDB, JsArray(Seq(Json.obj("example" -> JsNumber(1)))))
      when(mockQueryParser.generate(upsert, Map(testDB -> schema))).thenReturn(aqlString)

      when(mockConn.postControl(aqlString)).thenAnswer(new Answer[Future[Boolean]] {
        override def answer(invocation: InvocationOnMock): Future[Boolean] = {
          Thread.sleep(1000)
          Future(true)
        }
      })

      val agent = system.actorOf(MetaDataAgent.props(testDB, schema, mockQueryParser, mockConn, Config.Default))

      sender1.send(agent, upsert)
      sender2.send(agent, upsert)

      sender1.expectNoMsg(900 milli)
      sender1.expectMsg(1000 milli, true)
      sender2.expectNoMsg(900 milli)
      sender2.expectMsg(true)
      ok
    }
  }

}
