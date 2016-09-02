package edu.uci.ics.cloudberry.zion.actor

import java.util.concurrent.Executors

import akka.actor.{ActorRef, ActorRefFactory}
import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.TInterval
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, JSONParser, QueryPlanner, TestQuery}
import edu.uci.ics.cloudberry.zion.model.schema.{CreateView, Query, TimeField}
import edu.uci.ics.cloudberry.zion.model.util.MockConnClient
import org.joda.time.{DateTime, DateTimeZone}
import org.specs2.mock.Mockito
import org.specs2.mutable.SpecificationLike
import play.api.libs.json._
import org.specs2.mutable.Specification

import scala.concurrent.ExecutionContext

class ReactiveBerryClientTest extends TestkitExample with SpecificationLike with Mockito {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  import scala.concurrent.duration._
  import org.mockito.Mockito._

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
      sender.send(client, hourCountJSON)
      val askInfo = dataManager.receiveOne(5 seconds).asInstanceOf[DataStoreManager.AskInfo]
      askInfo.who must_== query.dataset

      dataManager.reply(Some(TestQuery.sourceInfo))

      def getRet(i: Int) = JsArray(Seq(JsObject(Seq("hour" -> JsNumber(i), "count" -> JsNumber(i)))))

      val slicedQ1 = worker.receiveOne(5 seconds).asInstanceOf[Query]
      val interval1 = slicedQ1.getTimeInterval("create_at").get
      interval1.getEnd must_== endTime
      interval1.toDurationMillis must_== (30 days).toMillis

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
    "cancel the haven't finished query if newer query comes" in {
      ok
    }
    "don't even start slice if the request is updated before info response gets back" in {
      ok
    }
  }
}
