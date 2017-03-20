package edu.uci.ics.cloudberry.zion.actor

import java.util.concurrent.Executors

import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{IDataConn, IQLGenerator}
import edu.uci.ics.cloudberry.zion.model.impl.TwitterDataStore
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.DateTime
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

  val schema = TwitterDataStore.TwitterSchema
  val minTimeResponse = Json.obj("min" -> JsString(TimeField.TimeFormat.print(new DateTime(2016, 1, 1, 0, 0))))
  val maxTimeResponse = Json.obj("max" -> JsString(TimeField.TimeFormat.print(new DateTime(2017, 1, 1, 0, 0))))
  val initialCount = 9876
  val countResponse = Json.obj("count" -> JsNumber(initialCount))

  "BaseDataSetAgent" should {
    "collect stats information periodically" in new TestkitExample {
      val sender = new TestProbe(system)
      val mockQueryParserSpecial = mock[IQLGenerator]
      val mockConnSpecial = mock[IDataConn]

      val initialAQL = "initial"
      val updateCountAQL = "update"
      when(mockQueryParserSpecial.generate(any, any))
        .thenReturn(initialAQL)
        .thenReturn(initialAQL)
        .thenReturn(initialAQL)
        .thenReturn(updateCountAQL) // update count

      when(mockConnSpecial.postQuery(initialAQL))
        .thenReturn(Future(minTimeResponse))
        .thenReturn(Future(maxTimeResponse))
        .thenReturn(Future(countResponse))

      when(mockConnSpecial.postQuery(updateCountAQL))
        .thenReturn(Future(countResponse))

      val updatePerSecondConfig = new Config(Configuration("agent.collect.stats.interval" -> "1 second"))
      val agent = system.actorOf(OriginalDataAgent.props("test", schema, mockQueryParserSpecial, mockConnSpecial, updatePerSecondConfig))
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
    "collect stats information when start" in new TestkitExample {
      val sender = new TestProbe(system)
      val mockQueryParser = mock[IQLGenerator]
      val mockConn = mock[IDataConn]

      val initialAQL = "initial"
      when(mockQueryParser.generate(any, any))
        .thenReturn(initialAQL)
        .thenReturn(initialAQL)
        .thenReturn(initialAQL)

      when(mockConn.postQuery(initialAQL))
        .thenReturn(Future(minTimeResponse))
        .thenReturn(Future(maxTimeResponse))
        .thenReturn(Future(countResponse))

      system.actorOf(OriginalDataAgent.props("test", schema, mockQueryParser, mockConn, Config.Default))
      sender.expectNoMsg(1 seconds)

      //initial 3 times AQL call
      import org.mockito.Mockito

      verify(mockQueryParser, Mockito.times(3)).generate(any, any)
      verify(mockConn, Mockito.times(3)).postQuery(any)
      ok
    }
    "answer query" in new TestkitExample {
      val sender = new TestProbe(system)
      val mockQueryParser = mock[IQLGenerator]
      val mockConn = mock[IDataConn]

      val initialAQL = "initial"
      val actualAQL = "actual"
      when(mockQueryParser.generate(any, any))
        .thenReturn(initialAQL)
        .thenReturn(initialAQL)
        .thenReturn(initialAQL)
        .thenReturn(actualAQL)

      when(mockConn.postQuery(initialAQL))
        .thenReturn(Future(minTimeResponse))
        .thenReturn(Future(maxTimeResponse))
        .thenReturn(Future(countResponse))

      val jsResponse = JsObject(Seq("a" -> JsNumber(1)))
      val query = Query("twitter")
      when(mockConn.postQuery(actualAQL)).thenReturn(Future(jsResponse))

      val agent = system.actorOf(OriginalDataAgent.props("test", schema, mockQueryParser, mockConn, Config.Default))
      sender.expectNoMsg(1 seconds)
      sender.send(agent, query)
      sender.expectMsg(jsResponse)
      ok
    }
    "answer estimable query using stats only" in new TestkitExample {
      val sender = new TestProbe(system)
      val mockQueryParser = mock[IQLGenerator]
      val mockConn = mock[IDataConn]

      val initialAQL = "initial"
      when(mockQueryParser.generate(any, any))
        .thenReturn(initialAQL)
        .thenReturn(initialAQL)
        .thenReturn(initialAQL)

      when(mockConn.postQuery(initialAQL))
        .thenReturn(Future(minTimeResponse))
        .thenReturn(Future(maxTimeResponse))
        .thenReturn(Future(countResponse))

      val globalCount = GlobalAggregateStatement(AggregateStatement(AllField, Count, Field.as(Count(AllField), "count")))
      val query = Query("twitter", globalAggr = Some(globalCount), isEstimable = true)

      val agent = system.actorOf(OriginalDataAgent.props("test", schema, mockQueryParser, mockConn, Config.Default))

      sender.expectNoMsg(1 seconds)
      sender.send(agent, query)
      val countResult = sender.receiveOne(1 seconds).asInstanceOf[JsValue]
      (countResult \\ "count").head.as[Int] must be_>=(initialCount)

      //initial 3 times AQL call
      import org.mockito.Mockito

      verify(mockQueryParser, Mockito.times(3)).generate(any, any)
      verify(mockConn, Mockito.times(3)).postQuery(any)
      ok
    }

  }
}
