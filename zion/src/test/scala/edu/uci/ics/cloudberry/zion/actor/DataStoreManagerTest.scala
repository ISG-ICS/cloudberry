package edu.uci.ics.cloudberry.zion.actor

import java.util.concurrent.Executors

import akka.actor.{ActorRef, ActorRefFactory, Props}
import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGeneratorFactory}
import edu.uci.ics.cloudberry.zion.model.impl.{AQLGenerator, DataSetInfo}
import edu.uci.ics.cloudberry.zion.model.schema._
import edu.uci.ics.cloudberry.zion.model.util.MockConnClient
import org.joda.time.DateTime
import org.specs2.mutable.SpecificationLike
import play.api.libs.json.{JsArray, Json}

import scala.concurrent.{ExecutionContext, Future}

class DataStoreManagerTest extends TestkitExample with SpecificationLike with MockConnClient {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  sequential

  import edu.uci.ics.cloudberry.zion.model.impl.TestQuery._
  import org.mockito.Mockito._

  import scala.concurrent.duration._

  val sender = new TestProbe(system)
  val child = new TestProbe(system)
  val meta = new TestProbe(system)
  val metaDataSet = "metaDataSet"

  def testActorMaker(context: ActorRefFactory, name: String, args: Seq[Any]): ActorRef = {
    if (name == "meta") meta.ref else child.ref
  }

  "DataManager" should {
    "load the meta info when preStart" in {
      val mockParserFactory = mock[IQLGeneratorFactory]
      val mockConn = mock[IDataConn]

      val initialInfo = JsArray(Seq(Json.toJson(sourceInfo)))
      val dataManager = system.actorOf(Props(new DataStoreManager(metaDataSet, mockConn, mockParserFactory, Config.Default, testActorMaker)))
      sender.send(dataManager, DataStoreManager.AreYouReady)
      val metaQuery = meta.receiveOne(5 seconds)
      metaQuery.asInstanceOf[Query].dataset must_== metaDataSet
      meta.reply(initialInfo)
      sender.expectMsg(true)
    }
    "answer the meta info" in {
      val mockParserFactory = mock[IQLGeneratorFactory]
      val mockConn = mock[IDataConn]

      val initialInfo = JsArray(Seq(Json.toJson(sourceInfo)))
      val dataManager = system.actorOf(Props(new DataStoreManager(metaDataSet, mockConn, mockParserFactory, Config.Default, testActorMaker)))
      val metaQuery = meta.receiveOne(5 seconds)
      metaQuery.asInstanceOf[Query].dataset must_== metaDataSet
      meta.reply(initialInfo)

      sender.send(dataManager, DataStoreManager.AskInfoAndViews(sourceInfo.name))
      val actual = sender.receiveOne(5 second)
      actual must_== Seq(sourceInfo)

      sender.send(dataManager, DataStoreManager.AskInfoAndViews("nobody"))
      sender.expectMsg(Seq.empty)
    }
    "forward the query to agent" in {
      val mockParserFactory = mock[IQLGeneratorFactory]
      val mockConn = mock[IDataConn]

      val initialInfo = JsArray(Seq(Json.toJson(sourceInfo)))
      val dataManager = system.actorOf(Props(new DataStoreManager(metaDataSet, mockConn, mockParserFactory, Config.Default, testActorMaker)))
      meta.receiveOne(5 seconds)
      meta.reply(initialInfo)

      val query = Query(dataset = sourceInfo.name)
      sender.send(dataManager, query)
      child.expectMsg(query)
      ok
    }
    "update meta info if create view succeeds" in {
      val now = DateTime.now()
      val parser = new AQLGenerator
      val mockParserFactory = mock[IQLGeneratorFactory]
      when(mockParserFactory.apply()).thenReturn(parser)

      val mockConn = mock[IDataConn]
      when(mockConn.postControl(any[String])).thenReturn(Future(true))

      val viewStatJson = JsArray(Seq(Json.obj("min" -> "2015-01-01T00:00:00.000Z", "max" -> "2016-01-01T00:00:00.000Z", "count" -> 2000)))
      when(mockConn.postQuery(any[String])).thenReturn(Future(viewStatJson))

      val initialInfo = JsArray(Seq(Json.toJson(sourceInfo)))
      val dataManager = system.actorOf(Props(new DataStoreManager(metaDataSet, mockConn, mockParserFactory, Config.Default, testActorMaker)))
      meta.receiveOne(5 seconds)
      meta.reply(initialInfo)

      sender.send(dataManager, DataStoreManager.AskInfoAndViews(sourceInfo.name))
      sender.expectMsg(Seq(sourceInfo))

      val createView = CreateView("zika", zikaCreateQuery)
      sender.send(dataManager, createView)
      sender.expectNoMsg(500 milli)
      val upsertRecord = meta.receiveOne(5 seconds)
      upsertRecord.asInstanceOf[UpsertRecord].dataset must_== metaDataSet
      sender.send(dataManager, DataStoreManager.AskInfoAndViews(sourceInfo.name))
      val response = sender.receiveOne(2000 milli).asInstanceOf[Seq[DataSetInfo]]
      response.size must_== 2
      response.head must_== sourceInfo
      val viewInfo = response.last
      viewInfo.name must_== createView.dataset
      viewInfo.createQueryOpt must_== Some(createView.query)
      viewInfo.schema must_== sourceInfo.schema
      viewInfo.dataInterval.getStart must_== TimeField.TimeFormat.parseDateTime((viewStatJson \\ "min").head.as[String])
      viewInfo.dataInterval.getEnd must_== TimeField.TimeFormat.parseDateTime((viewStatJson \\ "max").head.as[String])
      viewInfo.stats.cardinality must_== (viewStatJson \\ "count").head.as[Long]
      viewInfo.stats.lastModifyTime.getMillis must be_>= (now.getMillis)
      ok
    }
    "update meta stats if append view succeeds" in {
      val parser = new AQLGenerator
      val mockParserFactory = mock[IQLGeneratorFactory]
      when(mockParserFactory.apply()).thenReturn(parser)

      val now = DateTime.now()
      val mockConn = mock[IDataConn]
      val viewStatJson = JsArray(Seq(Json.obj("min" -> "2015-01-01T00:00:00.000Z", "max" -> "2016-01-01T00:00:00.000Z", "count" -> 2000)))
      when(mockConn.postQuery(any[String])).thenReturn(Future(viewStatJson))

      val initialInfo = Json.toJson(Seq(sourceInfo, zikaHalfYearViewInfo)).asInstanceOf[JsArray]
      val dataManager = system.actorOf(Props(new DataStoreManager(metaDataSet, mockConn, mockParserFactory, Config.Default, testActorMaker)))
      meta.receiveOne(3 seconds)
      meta.reply(initialInfo)

      sender.send(dataManager, DataStoreManager.AreYouReady)
      sender.expectMsg(true)
      sender.send(dataManager, DataStoreManager.AskInfoAndViews(sourceInfo.name))
      sender.expectMsg(Seq(sourceInfo, zikaHalfYearViewInfo))

      val appendView = AppendView(zikaHalfYearViewInfo.name, Query(sourceInfo.name))
      sender.send(dataManager, appendView)
      child.expectMsg(appendView)
      child.reply(true)
      sender.expectNoMsg(1 seconds)
      sender.send(dataManager, DataStoreManager.AskInfoAndViews(zikaHalfYearViewInfo.name))
      val newInfo = sender.receiveOne(1 second).asInstanceOf[Seq[DataSetInfo]].head
      newInfo.name must_== zikaHalfYearViewInfo.name
      newInfo.dataInterval.getEnd must_== TimeField.TimeFormat.parseDateTime((viewStatJson \\ "max").head.as[String])
      newInfo.stats.cardinality must_== (viewStatJson \\ "count").head.as[Long]
      newInfo.stats.lastModifyTime.getMillis must be_>= (now.getMillis)
    }
    "update meta info if receive drop request" in {
      ok
    }
    "use existing child to solve the query" in {
      ok
    }
  }
}
