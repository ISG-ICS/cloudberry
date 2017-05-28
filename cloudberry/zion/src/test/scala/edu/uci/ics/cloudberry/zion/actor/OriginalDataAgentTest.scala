package edu.uci.ics.cloudberry.zion.actor

import java.util.concurrent.Executors

import akka.testkit.{TestActorRef, TestProbe}
import edu.uci.ics.cloudberry.zion.actor.OriginalDataAgent.{Cardinality, NewStats, UpdateStats}
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.impl.{DataSetInfo, Stats, TwitterDataStore}
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.{DateTime, Interval}
import org.mockito.Mockito._
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import play.api.Configuration
import play.api.libs.json._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

class OriginalDataAgentTest extends Specification with Mockito {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  sequential

  "BaseDataSetAgent" should {
    val initialMinTime = new DateTime(2016, 1, 1, 0, 0)
    val initialMaxTime = new DateTime(2017, 1, 1, 0, 0)
    val initialCount = 9876

    val interval = new Interval(initialMinTime.getMillis, initialMaxTime.getMillis)
    val stats = Stats(new DateTime(), new DateTime(), new DateTime(), initialCount)
    val dataSetInfo = new DataSetInfo("test", None, TwitterDataStore.TwitterSchema, interval, stats)

    val countResponse = Json.obj("count" -> JsNumber(initialCount))

    "collect stats information when start" in new TestkitExample {
      val sender = new TestProbe(system)
      val mockQueryParser = mock[IQLGenerator]
      val mockConn = mock[IDataConn]

      val updateCountAQL = "update"
      when(mockQueryParser.generate(any, any))
        .thenReturn(updateCountAQL)
      when(mockConn.postQuery(updateCountAQL))
        .thenReturn(Future(countResponse))

      system.actorOf(OriginalDataAgent.props(dataSetInfo, mockQueryParser, mockConn, Config.Default))
      sender.expectNoMsg(1 seconds)

      //initial 1 times AQL call
      import org.mockito.Mockito
      verify(mockQueryParser, Mockito.times(1)).generate(any, any)
      verify(mockConn, Mockito.times(1)).postQuery(any)

      ok
    }
    "collect stats information periodically" in new TestkitExample {
      val sender = new TestProbe(system)
      val mockQueryParserSpecial = mock[IQLGenerator]
      val mockConnSpecial = mock[IDataConn]

      val updateCountAQL = "update"
      when(mockQueryParserSpecial.generate(any, any))
        .thenReturn(updateCountAQL)
      when(mockConnSpecial.postQuery(updateCountAQL))
        .thenReturn(Future(countResponse))

      val updatePerSecondConfig = new Config(Configuration("agent.collect.stats.interval" -> "1 second"))
      val agent = system.actorOf(OriginalDataAgent.props(dataSetInfo, mockQueryParserSpecial, mockConnSpecial, updatePerSecondConfig))
      sender.expectNoMsg(500 milli)

      val globalCount = GlobalAggregateStatement(AggregateStatement(AllField, Count, Field.as(Count(AllField), "count")))
      val query = Query("twitter", globalAggr = Some(globalCount), isEstimable = true)
      sender.send(agent, query)
      val countResult = (sender.receiveOne(500 millis).asInstanceOf[JsValue] \\ "count").head.as[Int]
      countResult must be_>=(initialCount)

      sender.expectNoMsg(1200 milli)
      sender.send(agent, query)
      val countResult2 = (sender.receiveOne(500 millis).asInstanceOf[JsValue] \\ "count").head.as[Int]
      countResult2 must be_>=(initialCount * 2)

      ok
    }
    "answer query" in new TestkitExample {
      val sender = new TestProbe(system)
      val mockQueryParser = mock[IQLGenerator]
      val mockConn = mock[IDataConn]

      val actualAQL = "actual"
      val jsResponse = JsObject(Seq("a" -> JsNumber(1)))

      when(mockQueryParser.generate(any, any))
        .thenReturn(actualAQL)
      when(mockConn.postQuery(actualAQL))
        .thenReturn(Future(jsResponse))

      val query = Query("twitter")
      val agent = system.actorOf(OriginalDataAgent.props(dataSetInfo, mockQueryParser, mockConn, Config.Default))

      sender.expectNoMsg(1 seconds)
      sender.send(agent, query)
      sender.expectMsg(jsResponse)

      ok
    }
    "send updated stats to data store manager" in new TestkitExample{
      val sender = new TestProbe(system)
      val parent = new TestProbe(system)

      val mockQueryParser = mock[IQLGenerator]
      val mockConn = mock[IDataConn]

      val updateCountAQL = "update"
      when(mockQueryParser.generate(any, any))
        .thenReturn(updateCountAQL)
      when(mockConn.postQuery(updateCountAQL))
        .thenReturn(Future(countResponse))

      val additionalCount = 111
      val dateNow = new DateTime()
      val cardinality = new Cardinality(initialMaxTime, dateNow, additionalCount)

      val agent = TestActorRef(OriginalDataAgent.props(dataSetInfo, mockQueryParser, mockConn, Config.Default), parent.ref, "child")
      parent.expectMsg(NewStats(dataSetInfo.name, initialCount))

      sender.send(agent, cardinality)
      parent.expectMsg(NewStats(dataSetInfo.name, additionalCount))

      sender.send(agent, UpdateStats)
      parent.expectMsg(NewStats(dataSetInfo.name, initialCount))

      ok
    }
    "answer estimable query using stats only" in new TestkitExample {
      val sender = new TestProbe(system)
      val mockQueryParser = mock[IQLGenerator]
      val mockConn = mock[IDataConn]

      val updateCountAQL = "update"
      when(mockQueryParser.generate(any, any))
        .thenReturn(updateCountAQL)
      when(mockConn.postQuery(updateCountAQL))
        .thenReturn(Future(countResponse))

      val globalCount = GlobalAggregateStatement(AggregateStatement(AllField, Count, Field.as(Count(AllField), "count")))
      val query = Query("twitter", globalAggr = Some(globalCount), isEstimable = true)

      val agent = system.actorOf(OriginalDataAgent.props(dataSetInfo, mockQueryParser, mockConn, Config.Default))

      sender.expectNoMsg(1 seconds)
      sender.send(agent, query)
      val countResult = sender.receiveOne(1 seconds).asInstanceOf[JsValue]
      (countResult \\ "count").head.as[Int] must be_>=(initialCount)

      //initial 1 time AQL call
      import org.mockito.Mockito
      verify(mockQueryParser, Mockito.times(1)).generate(any, any)
      verify(mockConn, Mockito.times(1)).postQuery(any)

      ok
    }
  }
}
