package edu.uci.ics.cloudberry.zion.actor

import java.util.concurrent.Executors

import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.impl.DataSetInfo
import edu.uci.ics.cloudberry.zion.model.impl.TestQuery._
import edu.uci.ics.cloudberry.zion.model.schema._
import edu.uci.ics.cloudberry.zion.model.util.MockConnClient
import org.mockito.stubbing.Answer
import org.specs2.mutable.SpecificationLike
import play.api.libs.json.{JsArray, JsNumber, Json}
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
      sender1.expectMsg(2000 milli, true)
      sender2.expectNoMsg(900 milli)
      sender2.expectMsg(true)
      ok
    }

    "forward delete record query" in {
      val sender = new TestProbe(system)
      val mockQueryParser = mock[IQLGenerator]
      val mockConn = mock[IDataConn]

      val aqlString = "mock aql"
      val testDB = "meta"
      val datasetFilter = FilterStatement(DataSetInfo.MetaSchema.fieldMap("name"), None, Relation.matches, Seq(TwitterDataSet))
      val delete = DeleteRecord(TwitterDataSet, Seq(datasetFilter))

      when(mockQueryParser.generate(delete, Map(testDB -> DataSetInfo.MetaSchema))).thenReturn(aqlString)
      when(mockConn.postControl(aqlString)).thenAnswer(new Answer[Future[Boolean]] {
        override def answer(invocation: InvocationOnMock): Future[Boolean] = {
          Thread.sleep(1000)
          Future(true)
        }
      })

      val agent = system.actorOf(MetaDataAgent.props(testDB, DataSetInfo.MetaSchema, mockQueryParser, mockConn, Config.Default))

      sender.send(agent, delete)
      sender.expectNoMsg(900 milli)
      sender.expectMsg(1000 milli, true)
      ok
    }

    "send delete record query when drop view" in {
      val sender = new TestProbe(system)
      val mockQueryParser = mock[IQLGenerator]
      val mockConn = mock[IDataConn]

      val aqlDropString = "mock aql drop"
      val aqlDeleteString = "mock aql delete"
      val testDB = "meta"

      val drop = DropView(TwitterDataSet)
      val viewRecordFilter = FilterStatement(DataSetInfo.MetaSchema.fieldMap("name"), None, Relation.matches, Seq(drop.dataset))
      val delete = DeleteRecord(DataSetInfo.MetaDataDBName, Seq(viewRecordFilter))

      when(mockQueryParser.generate(drop, Map())).thenReturn(aqlDropString)
      when(mockConn.postControl(aqlDropString)).thenAnswer(new Answer[Future[Boolean]] {
        override def answer(invocation: InvocationOnMock): Future[Boolean] = {
          Future(true)
        }
      })
      when(mockQueryParser.generate(delete, Map(testDB -> DataSetInfo.MetaSchema))).thenReturn(aqlDeleteString)
      when(mockConn.postControl(aqlDeleteString)).thenAnswer(new Answer[Future[Boolean]] {
        override def answer(invocation: InvocationOnMock): Future[Boolean] = {
          Future(true)
        }
      })

      val meta = system.actorOf(MetaDataAgent.props(testDB, DataSetInfo.MetaSchema, mockQueryParser, mockConn, Config.Default))
      sender.send(meta, drop)
      sender.expectMsg(true)
      ok
    }
  }

}
