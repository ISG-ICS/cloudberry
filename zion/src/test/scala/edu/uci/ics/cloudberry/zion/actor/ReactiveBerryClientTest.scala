package edu.uci.ics.cloudberry.zion.actor

import java.util.concurrent.Executors

import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.TInterval
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.impl.QueryPlanner.{IMerger, Unioner}
import edu.uci.ics.cloudberry.zion.model.impl.{JSONParser, QueryPlanner, TestQuery}
import edu.uci.ics.cloudberry.zion.model.schema.{CreateView, Query, QueryExeOption, TimeField}
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import play.api.libs.json._

import scala.concurrent.ExecutionContext

class ReactiveBerryClientTest extends TestkitExample with SpecificationLike with Mockito {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  import org.mockito.Mockito._

  import scala.concurrent.duration._

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

  def makeOptionJsonObj(json: JsValue): JsObject = {
    json.asInstanceOf[JsObject] + ("option" -> Json.obj(QueryExeOption.TagSliceMillis -> JsNumber(50)))
  }

  def getRet(i: Int) = JsArray(Seq(JsObject(Seq("hour" -> JsNumber(i), "count" -> JsNumber(i)))))

  sequential

  "Client" should {
    "slice the query into small pieces and return the merged result incrementally" in {

      val sender = new TestProbe(system)
      val dataManager = new TestProbe(system)
      val parser = new JSONParser
      val mockPlanner = mock[QueryPlanner]
      when(mockPlanner.calculateMergeFunc(any, any)).thenReturn(QueryPlanner.Unioner)
      //Return the input query
      when(mockPlanner.makePlan(any, any, any)).thenAnswer(new Answer[(Seq[Query], IMerger)] {
        override def answer(invocation: InvocationOnMock): (Seq[Query], IMerger) = {
          val query = invocation.getArguments().head.asInstanceOf[Query]
          (Seq(query), Unioner)
        }
      })

      val client = system.actorOf(BerryClient.props(parser, dataManager.ref, mockPlanner, Config.Default))

      sender.send(client, makeOptionJsonObj(hourCountJSON))
      val askInfo = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo.who must_== "twitter.ds_tweet"
      dataManager.reply(Some(TestQuery.sourceInfo))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))

      val slicedQ1 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval1 = slicedQ1.getTimeInterval("create_at").get
      interval1.getEnd must_== endTime
      interval1.toDurationMillis must_== Config.Default.FirstQueryTimeGap.toMillis

      dataManager.reply(getRet(1))
      sender.expectMsg(JsArray(Seq(getRet(1))))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))
      val slicedQ2 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval2 = slicedQ2.getTimeInterval("create_at").get
      println(interval2)
      interval2.getEnd must_== interval1.getStart
      interval2.getStartMillis must be_>=(startTime.getMillis)

      dataManager.reply(getRet(2))
      sender.expectMsg(JsArray(Seq(getRet(1) ++ getRet(2))))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))
      val slicedQ3 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval3 = slicedQ3.getTimeInterval("create_at").get
      println(interval3)
      interval3.getEnd must_== interval2.getStart
      interval3.getStartMillis must be_>=(startTime.getMillis)

      dataManager.reply(getRet(3))
      sender.expectMsg(JsArray(Seq(getRet(1) ++ getRet(2) ++ getRet(3))))
      ok
    }
    "suggest the view at the end of the last query finishes" in {
      val sender = new TestProbe(system)
      val dataManager = new TestProbe(system)
      val mockParser = new JSONParser
      val mockPlanner = mock[QueryPlanner]
      when(mockPlanner.calculateMergeFunc(any, any)).thenReturn(QueryPlanner.Unioner)
      //Return the input query
      when(mockPlanner.makePlan(any, any, any)).thenAnswer(new Answer[(Seq[Query], IMerger)] {
        override def answer(invocation: InvocationOnMock): (Seq[Query], IMerger) = {
          val query = invocation.getArguments().head.asInstanceOf[Query]
          (Seq(query), Unioner)
        }
      })

      import TestQuery.zikaCreateQuery
      val createView = CreateView("zika", zikaCreateQuery)
      when(mockPlanner.suggestNewView(any, any, any)).thenReturn(Seq(createView))

      val client = system.actorOf(BerryClient.props(mockParser, dataManager.ref, mockPlanner, Config.Default))

      val (query, _) = mockParser.parse(hourCountJSON)
      sender.send(client, makeOptionJsonObj(hourCountJSON))
      val askInfo = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo.who must_== query.head.dataset

      dataManager.reply(Some(TestQuery.sourceInfo))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))

      var intervalx = new TInterval(DateTime.now(), DateTime.now())
      var response = JsArray()
      var qx: Query = Query("dataset")

      while (intervalx.getStartMillis > startTime.getMillis) {
        qx = dataManager.receiveOne(5 second).asInstanceOf[Query]
        intervalx = qx.getTimeInterval("create_at").get
        dataManager.reply(getRet(0))
        response ++= getRet(0)
        sender.expectMsg(JsArray(Seq(response)))

        dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
        dataManager.reply(Seq(TestQuery.sourceInfo))
      }
      qx.getTimeInterval("create_at").get.getStartMillis must_== startTime.getMillis

      dataManager.expectMsg(createView)
      ok
    }
    "cancel the haven't finished query if newer query comes" in {
      val sender = new TestProbe(system)
      val dataManager = new TestProbe(system)
      val mockParser = new JSONParser
      val mockPlanner = mock[QueryPlanner]
      when(mockPlanner.calculateMergeFunc(any, any)).thenReturn(QueryPlanner.Unioner)
      //Return the input query
      when(mockPlanner.makePlan(any, any, any)).thenAnswer(new Answer[(Seq[Query], IMerger)] {
        override def answer(invocation: InvocationOnMock): (Seq[Query], IMerger) = {
          val query = invocation.getArguments().head.asInstanceOf[Query]
          (Seq(query), Unioner)
        }
      })

      val client = system.actorOf(BerryClient.props(mockParser, dataManager.ref, mockPlanner, Config.Default))

      val (query, _) = mockParser.parse(hourCountJSON)

      sender.send(client, makeOptionJsonObj(hourCountJSON))
      val askInfo = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo.who must_== query.head.dataset
      dataManager.reply(Some(TestQuery.sourceInfo))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))
      val slicedQ1 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval1 = slicedQ1.getTimeInterval("create_at").get
      interval1.getEnd must_== endTime
      interval1.toDurationMillis must_== Config.Default.FirstQueryTimeGap.toMillis

      dataManager.reply(getRet(1))
      sender.expectMsg(JsArray(Seq(getRet(1))))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))
      val slicedQ2 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval2 = slicedQ2.getTimeInterval("create_at").get
      println(interval2)
      interval2.getEnd must_== interval1.getStart
      interval2.getStartMillis must be_>=(startTime.getMillis)

      //send a new request
      sender.send(client, makeOptionJsonObj(hourCountJSON2))
      Thread.sleep(250)
      dataManager.reply(getRet(2))

      sender.expectNoMsg()
      val askInfo2 = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo2.who must_== (hourCountJSON2 \ "dataset").as[String]
      dataManager.reply(Some(TestQuery.sourceInfo))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))
      val slicedQ11 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval11 = slicedQ11.getTimeInterval("create_at").get
      interval11.getEnd must_== endTime2
      interval11.toDurationMillis must_== Config.Default.FirstQueryTimeGap.toMillis

      dataManager.reply(getRet(1))
      sender.expectMsg(JsArray(Seq(getRet(1))))
      ok
    }
    "don't even start slice if the request is updated before info response gets back" in {
      val sender = new TestProbe(system)
      val dataManager = new TestProbe(system)
      val mockParser = new JSONParser
      val mockPlanner = mock[QueryPlanner]
      when(mockPlanner.calculateMergeFunc(any, any)).thenReturn(QueryPlanner.Unioner)
      //Return the input query
      when(mockPlanner.makePlan(any, any, any)).thenAnswer(new Answer[(Seq[Query], IMerger)] {
        override def answer(invocation: InvocationOnMock): (Seq[Query], IMerger) = {
          val query = invocation.getArguments().head.asInstanceOf[Query]
          (Seq(query), Unioner)
        }
      })

      val client = system.actorOf(BerryClient.props(mockParser, dataManager.ref, mockPlanner, Config.Default))

      val (query, _) = mockParser.parse(hourCountJSON)

      sender.send(client, makeOptionJsonObj(hourCountJSON))
      val askInfo = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo.who must_== query.head.dataset

      //new query comes before the worker even started
      sender.send(client, makeOptionJsonObj(hourCountJSON2))

      dataManager.reply(Some(TestQuery.sourceInfo))

      sender.expectNoMsg(1 seconds)

      val askInfo2 = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo2.who must_== (hourCountJSON2 \ "dataset").as[String]
      dataManager.reply(Some(TestQuery.sourceInfo))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))
      val slicedQ1 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval1 = slicedQ1.getTimeInterval("create_at").get
      interval1.getEnd must_== endTime2
      interval1.toDurationMillis must_== Config.Default.FirstQueryTimeGap.toMillis

      dataManager.reply(getRet(1))
      sender.expectMsg(JsArray(Seq(getRet(1))))

      ok
    }
  }
}
