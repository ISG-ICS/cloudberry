package edu.uci.ics.cloudberry.zion.model.actor

import java.util.concurrent.Executors

import akka.actor.{ActorRef, ActorRefFactory, Props}
import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.actor.TestkitExample
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQueryParser, IQueryParserFactory}
import edu.uci.ics.cloudberry.zion.model.impl.{AQLQueryParser, DataSetInfo}
import edu.uci.ics.cloudberry.zion.model.schema.{AppendView, CreateView, Query}
import edu.uci.ics.cloudberry.zion.model.util.MockConnClient
import org.specs2.mutable.SpecificationLike

import scala.concurrent.{ExecutionContext, Future}

class DataManagerTest extends TestkitExample with SpecificationLike with MockConnClient {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  sequential

  import org.mockito.Mockito._
  import edu.uci.ics.cloudberry.zion.model.impl.TestQuery._
  import scala.concurrent.duration._

  val sender = new TestProbe(system)
  val child = new TestProbe(system)

  def testActorMaker(context: ActorRefFactory, name: String, args: Seq[Any]): ActorRef = child.ref

  "DataManager" should {
    "answer the meta info" in {
      val mockParserFactory = mock[IQueryParserFactory]
      val mockConn = mock[IDataConn]

      val initialInfo: Map[String, DataSetInfo] = Map(sourceInfo.name -> sourceInfo)
      val dataManager = system.actorOf(Props(new DataManager(initialInfo, mockConn, mockParserFactory, Config.Default, testActorMaker)))
      sender.send(dataManager, DataManager.AskInfoMsg(sourceInfo.name))
      val actual = sender.receiveOne(1 second)
      actual must_== Seq(sourceInfo)

      sender.send(dataManager, DataManager.AskInfoMsg("nobody"))
      sender.expectMsg(Seq.empty)
    }
    "forward the query to agent" in {
      val mockParserFactory = mock[IQueryParserFactory]
      val mockConn = mock[IDataConn]

      val initialInfo: Map[String, DataSetInfo] = Map(sourceInfo.name -> sourceInfo)
      val dataManager = system.actorOf(Props(new DataManager(initialInfo, mockConn, mockParserFactory, Config.Default, testActorMaker)))
      val query = Query(dataset = sourceInfo.name)
      sender.send(dataManager, query)
      child.expectMsg(query)
      ok
    }
    "update meta info if create view succeeds" in {
      val parser = new AQLQueryParser
      val mockParserFactory = mock[IQueryParserFactory]
      when(mockParserFactory.apply()).thenReturn(parser)

      val mockConn = mock[IDataConn]
      when(mockConn.postControl(any[String])).thenReturn(Future(true))

      val initialInfo: Map[String, DataSetInfo] = Map(sourceInfo.name -> sourceInfo)
      val dataManager = system.actorOf(Props(new DataManager(initialInfo, mockConn, mockParserFactory, Config.Default, testActorMaker)))

      sender.send(dataManager, DataManager.AskInfoMsg(sourceInfo.name))
      sender.expectMsg(Seq(sourceInfo))

      val createView = CreateView("zika", zikaCreateQuery)
      sender.send(dataManager, createView)
      sender.expectNoMsg(500 milli)
      sender.send(dataManager, DataManager.AskInfoMsg(sourceInfo.name))
      val response = sender.receiveOne(2000 milli).asInstanceOf[Seq[DataSetInfo]]
      response.size must_== 2
      response.head must_== sourceInfo
      response.last.name must_== createView.dataset
      response.last.createQueryOpt must_== Some(createView.query)
      response.last.schema must_== sourceInfo.schema
    }
    "update meta stats if append view succeeds" in {
      val parser = new AQLQueryParser
      val mockParserFactory = mock[IQueryParserFactory]
      when(mockParserFactory.apply()).thenReturn(parser)

      val mockConn = mock[IDataConn]
      val initialInfo = Map(sourceInfo.name -> sourceInfo, zikaHalfYearViewInfo.name -> zikaHalfYearViewInfo)
      val dataManager = system.actorOf(Props(new DataManager(initialInfo, mockConn, mockParserFactory, Config.Default, testActorMaker)))

      sender.send(dataManager, DataManager.AskInfoMsg(sourceInfo.name))
      sender.expectMsg(Seq(sourceInfo, zikaHalfYearViewInfo))

      val appendView = AppendView(zikaHalfYearViewInfo.name, Query(sourceInfo.name))
      sender.send(dataManager, appendView)
      child.expectMsg(appendView)
      child.reply(true)
      sender.expectNoMsg(100 milli)
      sender.send(dataManager, DataManager.AskInfoMsg(zikaHalfYearViewInfo.name))
      val newInfo = sender.receiveOne(1 second).asInstanceOf[Seq[DataSetInfo]].head
      newInfo.name must_== zikaHalfYearViewInfo.name
      newInfo.dataInterval.getEndMillis must be_> (zikaHalfYearViewInfo.dataInterval.getEndMillis)
      newInfo.stats.cardinality must be_> (zikaHalfYearViewInfo.stats.cardinality)
    }
    "update meta info if receive drop request" in {
      ok
    }
  }
}
