package edu.uci.ics.cloudberry.zion.actor

import java.util.concurrent.Executors

import akka.actor.{ActorRef, ActorRefFactory}
import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.TInterval
import edu.uci.ics.cloudberry.zion.actor.DataStoreManager.AskInfoAndViews
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.impl.{JSONParser, QueryPlanner, TestQuery}
import edu.uci.ics.cloudberry.zion.model.schema.{CreateView, Query, TimeField}
import org.joda.time.{DateTime, DateTimeZone}
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import play.api.libs.json._

import scala.concurrent.ExecutionContext

class ReactiveBerryClientTest extends TestkitExample with SpecificationLike with Mockito {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  import org.mockito.Mockito._

  import scala.concurrent.duration._
  import ReactiveBerryClient.NoTransform

  DateTimeZone.setDefault(DateTimeZone.UTC)
  val startTime = new DateTime(2016, 1, 1, 0, 0)
  val endTime = new DateTime(2016, 12, 31, 0, 0)
  val hourCountJSON = Json.parse(
    s"""
       |{
       |  "dataset": "twitter.ds_tweet",
       |  "filter": [
       |  {
       |    "field": "create_at",
       |    "relation": "inRange",
       |    "values": [
       |      "${TimeField.TimeFormat.print(startTime)}",
       |      "${TimeField.TimeFormat.print(endTime)}"
       |    ]
       |  }],
       |  "group": {
       |    "by": [
       |      {
       |        "field": "create_at",
       |        "apply": {
       |          "name": "interval",
       |          "args" : {
       |            "unit": "hour"
       |          }
       |        },
       |        "as": "hour"
       |      }
       |    ],
       |    "aggregate": [
       |      {
       |        "field": "*",
       |        "apply": {
       |          "name" : "count"
       |        },
       |        "as": "count"
       |      }
       |    ]
       |  }
       |}
    """.stripMargin)

  val endTime2 = new DateTime(2016, 11, 30, 0, 0)
  val hourCountJSON2 = Json.parse(
    s"""
       |{
       |  "dataset": "twitter.ds_tweet",
       |  "filter": [
       |  {
       |    "field": "create_at",
       |    "relation": "inRange",
       |    "values": [
       |      "${TimeField.TimeFormat.print(startTime)}",
       |      "${TimeField.TimeFormat.print(endTime2)}"
       |    ]
       |  },
       |  {
       |    "field": "geo_tag.stateID",
       |    "relation": "in",
       |    "values": [42]
       |  }
       |  ],
       |  "group": {
       |    "by": [
       |      {
       |        "field": "create_at",
       |        "apply": {
       |          "name": "interval",
       |          "args" : {
       |            "unit": "hour"
       |          }
       |        },
       |        "as": "hour"
       |      }
       |    ],
       |    "aggregate": [
       |      {
       |        "field": "*",
       |        "apply": {
       |          "name" : "count"
       |        },
       |        "as": "count"
       |      }
       |    ]
       |  }
       |}
    """.stripMargin)

  sequential

  "Client" should {
    "slice the query into small pieces and return the merged result incrementally" in {

      val sender = new TestProbe(system)
      val dataManager = new TestProbe(system)
      val worker = new TestProbe(system)
      val mockParser = new JSONParser
      val mockPlanner = mock[QueryPlanner]
      when(mockPlanner.calculateMergeFunc(any, any)).thenReturn(QueryPlanner.Unioner)
      val responseTime = 50

      def childMaker(context: ActorRefFactory, client: ReactiveBerryClient): ActorRef = worker.ref

      val client = system.actorOf(ReactiveBerryClient.props(mockParser, dataManager.ref, mockPlanner, Config.Default, childMaker, responseTime))

      val query = mockParser.parse(hourCountJSON)
      sender.send(client, ReactiveBerryClient.Request(Seq((hourCountJSON, NoTransform))))
      val askInfo = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo.who must_== query.dataset

      dataManager.reply(Some(TestQuery.sourceInfo))

      def getRet(i: Int) = JsArray(Seq(JsObject(Seq("hour" -> JsNumber(i), "count" -> JsNumber(i)))))

      val slicedQ1 = worker.receiveOne(5 seconds).asInstanceOf[Query]
      val interval1 = slicedQ1.getTimeInterval("create_at").get
      interval1.getEnd must_== endTime
      interval1.toDurationMillis must_== Config.Default.FirstQueryTimeGap.toMillis

      worker.reply(getRet(1))
      sender.expectMsg(getRet(1))

      val slicedQ2 = worker.receiveOne(5 seconds).asInstanceOf[Query]
      val interval2 = slicedQ2.getTimeInterval("create_at").get
      println(interval2)
      interval2.getEnd must_== interval1.getStart
      interval2.getStartMillis must be_>=(startTime.getMillis)

      worker.reply(getRet(2))
      sender.expectMsg(getRet(1) ++ getRet(2))

      val slicedQ3 = worker.receiveOne(5 seconds).asInstanceOf[Query]
      val interval3 = slicedQ3.getTimeInterval("create_at").get
      println(interval3)
      interval3.getEnd must_== interval2.getStart
      interval3.getStartMillis must be_>=(startTime.getMillis)

      worker.reply(getRet(3))
      sender.expectMsg(getRet(1) ++ getRet(2) ++ getRet(3))
      ok
    }
    "suggest the view at the end of the last query finishes" in {
      val sender = new TestProbe(system)
      val dataManager = new TestProbe(system)
      val worker = new TestProbe(system)
      val mockParser = new JSONParser
      val mockPlanner = mock[QueryPlanner]
      when(mockPlanner.calculateMergeFunc(any, any)).thenReturn(QueryPlanner.Unioner)

      import TestQuery.zikaCreateQuery
      val createView = CreateView("zika", zikaCreateQuery)
      when(mockPlanner.suggestNewView(any, any, any)).thenReturn(Seq(createView))
      val responseTime = 50

      def childMaker(context: ActorRefFactory, client: ReactiveBerryClient): ActorRef = worker.ref

      val client = system.actorOf(ReactiveBerryClient.props(mockParser, dataManager.ref, mockPlanner, Config.Default, childMaker, responseTime))

      val query = mockParser.parse(hourCountJSON)
      sender.send(client, ReactiveBerryClient.Request(Seq((hourCountJSON, NoTransform))))
      val askInfo = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo.who must_== query.dataset

      dataManager.reply(Some(TestQuery.sourceInfo))

      def getRet(i: Int) = JsArray(Seq(JsObject(Seq("hour" -> JsNumber(i), "count" -> JsNumber(i)))))

      var intervalx = new TInterval(DateTime.now(), DateTime.now())
      var response = JsArray()
      var qx : Query = Query("dataset")

      while (intervalx.getStartMillis > startTime.getMillis) {
        qx = worker.receiveOne(5 second).asInstanceOf[Query]
        intervalx = qx.getTimeInterval("create_at").get
        worker.reply(getRet(0))
        response ++= getRet(0)
        sender.expectMsg(response)
      }
      qx.getTimeInterval("create_at").get.getStartMillis must_== startTime.getMillis

      dataManager.expectMsg(AskInfoAndViews(query.dataset))
      dataManager.reply(Seq(TestQuery.sourceInfo))
      dataManager.expectMsg(createView)
      ok
    }
    "cancel the haven't finished query if newer query comes" in {
      val sender = new TestProbe(system)
      val dataManager = new TestProbe(system)
      val worker = new TestProbe(system)
      val mockParser = new JSONParser
      val mockPlanner = mock[QueryPlanner]
      when(mockPlanner.calculateMergeFunc(any, any)).thenReturn(QueryPlanner.Unioner)
      val responseTime = 50

      def childMaker(context: ActorRefFactory, client: ReactiveBerryClient): ActorRef = worker.ref

      val client = system.actorOf(ReactiveBerryClient.props(mockParser, dataManager.ref, mockPlanner, Config.Default, childMaker, responseTime))

      val query = mockParser.parse(hourCountJSON)

      sender.send(client, ReactiveBerryClient.Request(Seq((hourCountJSON, NoTransform))))
      val askInfo = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo.who must_== query.dataset
      dataManager.reply(Some(TestQuery.sourceInfo))

      def getRet(i: Int) = JsArray(Seq(JsObject(Seq("hour" -> JsNumber(i), "count" -> JsNumber(i)))))

      val slicedQ1 = worker.receiveOne(5 seconds).asInstanceOf[Query]
      val interval1 = slicedQ1.getTimeInterval("create_at").get
      interval1.getEnd must_== endTime
      interval1.toDurationMillis must_== Config.Default.FirstQueryTimeGap.toMillis

      worker.reply(getRet(1))
      sender.expectMsg(getRet(1))

      val slicedQ2 = worker.receiveOne(5 seconds).asInstanceOf[Query]
      val interval2 = slicedQ2.getTimeInterval("create_at").get
      println(interval2)
      interval2.getEnd must_== interval1.getStart
      interval2.getStartMillis must be_>=(startTime.getMillis)

      //send a new request
      sender.send(client, ReactiveBerryClient.Request(Seq((hourCountJSON2, NoTransform))))
      Thread.sleep(250)
      worker.reply(getRet(2))

      sender.expectNoMsg()
      val askInfo2 = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo2.who must_== (hourCountJSON2 \ "dataset").as[String]
      dataManager.reply(Some(TestQuery.sourceInfo))

      val slicedQ11 = worker.receiveOne(5 seconds).asInstanceOf[Query]
      val interval11 = slicedQ11.getTimeInterval("create_at").get
      interval11.getEnd must_== endTime2
      interval11.toDurationMillis must_== Config.Default.FirstQueryTimeGap.toMillis

      worker.reply(getRet(1))
      sender.expectMsg(getRet(1))
      ok
    }
    "don't even start slice if the request is updated before info response gets back" in {
      val sender = new TestProbe(system)
      val dataManager = new TestProbe(system)
      val worker = new TestProbe(system)
      val mockParser = new JSONParser
      val mockPlanner = mock[QueryPlanner]
      when(mockPlanner.calculateMergeFunc(any, any)).thenReturn(QueryPlanner.Unioner)
      val responseTime = 50

      def childMaker(context: ActorRefFactory, client: ReactiveBerryClient): ActorRef = worker.ref

      val client = system.actorOf(ReactiveBerryClient.props(mockParser, dataManager.ref, mockPlanner, Config.Default, childMaker, responseTime))

      val query = mockParser.parse(hourCountJSON)

      sender.send(client, ReactiveBerryClient.Request(Seq((hourCountJSON, NoTransform))))
      val askInfo = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo.who must_== query.dataset

      //new query comes before the worker even started
      sender.send(client, ReactiveBerryClient.Request(Seq((hourCountJSON2, NoTransform))))

      dataManager.reply(Some(TestQuery.sourceInfo))

      worker.expectNoMsg(1 seconds)
      sender.expectNoMsg(1 seconds)

      val askInfo2 = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo2.who must_== (hourCountJSON2 \ "dataset").as[String]
      dataManager.reply(Some(TestQuery.sourceInfo))

      def getRet(i: Int) = JsArray(Seq(JsObject(Seq("hour" -> JsNumber(i), "count" -> JsNumber(i)))))

      val slicedQ1 = worker.receiveOne(5 seconds).asInstanceOf[Query]
      val interval1 = slicedQ1.getTimeInterval("create_at").get
      interval1.getEnd must_== endTime2
      interval1.toDurationMillis must_== Config.Default.FirstQueryTimeGap.toMillis

      worker.reply(getRet(1))
      sender.expectMsg(getRet(1))

      ok
    }
  }
}
