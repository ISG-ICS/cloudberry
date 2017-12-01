package edu.uci.ics.cloudberry.zion.actor

import java.util.concurrent.Executors

import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.TInterval
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.impl.QueryPlanner.{IMerger, Unioner}
import edu.uci.ics.cloudberry.zion.model.impl.{JSONParser, QueryPlanner, TestQuery}
import edu.uci.ics.cloudberry.zion.model.schema
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.{DateTime, DateTimeZone}
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import play.api.libs.json._

import scala.concurrent.ExecutionContext

class ReactiveBerryClientTest extends TestkitExample with SpecificationLike with Mockito {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(4))

  import org.mockito.Mockito._
  import scala.concurrent.duration._
  import TestQuery.twitterSchemaMap

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

  val dayCountJSON = Json.parse(
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
       |            "unit": "day"
       |          }
       |        },
       |        "as": "day"
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

  def getGroupAs(query: Query): Option[Field] =
    query.groups.get.bys(0).as

  "Client" should {
    "slice the query into mini-queries and return the merged result incrementally" in {

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

      val client = system.actorOf(BerryClient.props(parser, dataManager.ref, mockPlanner, Config.Default, sender.ref))

      sender.send(client, makeOptionJsonObj(hourCountJSON))
      val askInfo = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo.who must_== "twitter.ds_tweet"
      dataManager.reply(Some(TestQuery.sourceInfo))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))

      val slicedQ1 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval1 = slicedQ1.getTimeInterval(TimeField("create_at")).get
      interval1.getEnd must_== endTime
      interval1.toDurationMillis must_== Config.Default.FirstQueryTimeGap.toMillis

      dataManager.reply(getRet(1))
      sender.expectMsg(JsArray(Seq(getRet(1))))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))
      val slicedQ2 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval2 = slicedQ2.getTimeInterval(TimeField("create_at")).get
      println(interval2)
      interval2.getEnd must_== interval1.getStart
      interval2.getStartMillis must be_>=(startTime.getMillis)

      dataManager.reply(getRet(2))
      sender.expectMsg(JsArray(Seq(getRet(1) ++ getRet(2))))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))
      val slicedQ3 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval3 = slicedQ3.getTimeInterval(TimeField("create_at")).get
      println(interval3)
      interval3.getEnd must_== interval2.getStart
      interval3.getStartMillis must be_>=(startTime.getMillis)

      dataManager.reply(getRet(3))
      sender.expectMsg(JsArray(Seq(getRet(1) ++ getRet(2) ++ getRet(3))))
      ok
    }

    "slice a query batch should generate a slice for each query" in {
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

      val client = system.actorOf(BerryClient.props(parser, dataManager.ref, mockPlanner, Config.Default, sender.ref))
      sender.send(client, makeOptionJsonObj(JsObject(Seq("batch" -> JsArray(Seq(hourCountJSON, dayCountJSON))))))

      val askInfo = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo.who must_== "twitter.ds_tweet"
      dataManager.reply(Some(TestQuery.sourceInfo))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))

      val slicedQ1 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval1 = slicedQ1.getTimeInterval(TimeField("create_at")).get
      interval1.getEnd must_== endTime
      interval1.toDurationMillis must_== Config.Default.FirstQueryTimeGap.toMillis

      dataManager.reply(
        getGroupAs(slicedQ1) match {
          case Some(TimeField("hour", _)) => getRet(1)
          case Some(TimeField("day", _)) => getRet(2)
        }
      )

      val slicedQ2 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval2 = slicedQ2.getTimeInterval(TimeField("create_at")).get
      interval2.getEnd must_== endTime
      interval2.toDurationMillis must_== Config.Default.FirstQueryTimeGap.toMillis

      dataManager.reply(
        getGroupAs(slicedQ2) match {
          case Some(TimeField("hour", _)) => getRet(1)
          case Some(TimeField("day", _)) => getRet(2)
        }
      )
      sender.expectMsg(JsArray(Seq(getRet(1), getRet(2))))

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

      val client = system.actorOf(BerryClient.props(mockParser, dataManager.ref, mockPlanner, Config.Default, sender.ref))

      val (query, _) = mockParser.parse(hourCountJSON, twitterSchemaMap)
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
        intervalx = qx.getTimeInterval(TimeField("create_at")).get
        dataManager.reply(getRet(0))
        response ++= getRet(0)
        sender.expectMsg(JsArray(Seq(response)))

        dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
        dataManager.reply(Seq(TestQuery.sourceInfo))
      }
      qx.getTimeInterval(TimeField("create_at")).get.getStartMillis must_== startTime.getMillis

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

      val client = system.actorOf(BerryClient.props(mockParser, dataManager.ref, mockPlanner, Config.Default, sender.ref))

      val (query, _) = mockParser.parse(hourCountJSON, twitterSchemaMap)

      sender.send(client, makeOptionJsonObj(hourCountJSON))
      val askInfo = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo.who must_== query.head.dataset
      dataManager.reply(Some(TestQuery.sourceInfo))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))
      val slicedQ1 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval1 = slicedQ1.getTimeInterval(TimeField("create_at")).get
      interval1.getEnd must_== endTime
      interval1.toDurationMillis must_== Config.Default.FirstQueryTimeGap.toMillis

      dataManager.reply(getRet(1))
      sender.expectMsg(JsArray(Seq(getRet(1))))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))
      val slicedQ2 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval2 = slicedQ2.getTimeInterval(TimeField("create_at")).get
      interval2.getEnd must_== interval1.getStart
      interval2.getStartMillis must be_>=(startTime.getMillis)

      //send a new request
      sender.send(client, makeOptionJsonObj(hourCountJSON2))
      val askInfo2 = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo2.who must_== (hourCountJSON2 \ "dataset").as[String]
      dataManager.reply(Some(TestQuery.sourceInfo))

      Thread.sleep(150)
      dataManager.reply(getRet(2))
      sender.expectNoMsg()

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))
      val slicedQ11 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval11 = slicedQ11.getTimeInterval(TimeField("create_at")).get
      interval11.getEnd must_== endTime2
      interval11.toDurationMillis must_== Config.Default.FirstQueryTimeGap.toMillis

      dataManager.reply(getRet(1))
      sender.expectMsg(JsArray(Seq(getRet(1))))
      ok
    }
    "stop when the number of returning results reached the limit" in {
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

      val client = system.actorOf(BerryClient.props(parser, dataManager.ref, mockPlanner, Config.Default, sender.ref))

      val selectJson = Json.obj(
        "order" -> Seq(JsString("-count")),
        "limit" -> JsNumber(2),
        "offset" -> JsNumber(0)
      )
      val optionJson = Json.obj(
        QueryExeOption.TagSliceMillis -> JsNumber(50)
      )

      sender.send(client, hourCountJSON.as[JsObject] + ("option" -> optionJson) + ("select" -> selectJson))
      val askInfo = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo.who must_== "twitter.ds_tweet"
      dataManager.reply(Some(TestQuery.sourceInfo))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))

      val slicedQ1 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval1 = slicedQ1.getTimeInterval(TimeField("create_at")).get
      interval1.getEnd must_== endTime
      interval1.toDurationMillis must_== Config.Default.FirstQueryTimeGap.toMillis

      dataManager.reply(getRet(1))
      sender.expectMsg(JsArray(Seq(getRet(1))))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))
      val slicedQ2 = dataManager.receiveOne(5 seconds).asInstanceOf[Query]
      val interval2 = slicedQ2.getTimeInterval(TimeField("create_at")).get
      interval2.getEnd must_== interval1.getStart
      interval2.getStartMillis must be_>=(startTime.getMillis)

      dataManager.reply(JsArray(Seq(
        Json.obj("hour" -> 2, "count" -> 2),
        Json.obj("hour" -> 3, "count" -> 3),
        Json.obj("hour" -> 4, "count" -> 4)
      )))
      sender.expectMsg(JsArray(Seq(getRet(1) ++ getRet(2))))

      dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfoAndViews]
      dataManager.reply(Seq(TestQuery.sourceInfo))
      dataManager.expectNoMsg()
      sender.expectMsg(BerryClient.Done)
      ok
    }
  }
}
