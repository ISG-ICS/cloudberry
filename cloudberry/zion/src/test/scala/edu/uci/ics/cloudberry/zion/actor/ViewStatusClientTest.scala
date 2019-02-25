package edu.uci.ics.cloudberry.zion.actor

import java.util.concurrent.Executors

import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.impl.{JSONParser, QueryPlanner}
import edu.uci.ics.cloudberry.zion.model.schema.{CreateView, Query, QueryExeOption}
import edu.uci.ics.cloudberry.zion.model.util.MockConnClient
import org.specs2.mutable.SpecificationLike
import play.api.libs.json._

import scala.concurrent.ExecutionContext

class ViewStatusClientTest extends TestkitExample with SpecificationLike with MockConnClient {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  import edu.uci.ics.cloudberry.zion.model.impl.TestQuery._
  import org.mockito.Mockito._

  sequential

  "ViewStatusClient" should {
    // A test to check whether query can be solved by view
    // The expected result in this test is true
    "send query to dataManager to check whether it can be solved by view, return true" in {
      val sender = new TestProbe(system)
      val dataManager = new TestProbe(system)
      val mockParser = mock[JSONParser]
      val mockPlanner = mock[QueryPlanner]

      val jsonRequest = JsObject(Seq("fake" -> JsNumber(1)))
      val query = Query(sourceInfo.name)
      when(mockParser.parse(jsonRequest, twitterSchemaMap)).thenReturn((Seq(query), QueryExeOption.NoSliceNoContinue))
      when(mockParser.getDatasets(jsonRequest)).thenReturn(Set(TwitterDataSet))

      val client = system.actorOf(ViewStatusClient.props(mockParser, dataManager.ref, mockPlanner, Config.Default, sender.ref))
      sender.send(client, jsonRequest)

      dataManager.expectMsg(DataStoreManager.AskInfo(query.dataset))
      dataManager.reply(Some(sourceInfo))

      dataManager.expectMsg(DataStoreManager.AskInfoAndViews(query.dataset))
      dataManager.reply(Seq(sourceInfo))

      when(mockPlanner.requestViewForQuery(query, sourceInfo, Seq.empty)).thenReturn(true)

      sender.expectMsg(JsArray(Seq(JsBoolean(true))))

      ok
    }

    // A test to check whether query can be solved by view
    // The expected result in this test is false
    "send query to dataManager to check whether it can be solved by view, return false" in {
      val sender = new TestProbe(system)
      val dataManager = new TestProbe(system)
      val mockParser = mock[JSONParser]
      val mockPlanner = mock[QueryPlanner]

      val jsonRequest = JsObject(Seq("fake" -> JsNumber(1)))
      val query = Query(sourceInfo.name)
      when(mockParser.parse(jsonRequest, twitterSchemaMap)).thenReturn((Seq(query), QueryExeOption.NoSliceNoContinue))
      when(mockParser.getDatasets(jsonRequest)).thenReturn(Set(TwitterDataSet))

      val client = system.actorOf(ViewStatusClient.props(mockParser, dataManager.ref, mockPlanner, Config.Default, sender.ref))
      sender.send(client, jsonRequest)

      dataManager.expectMsg(DataStoreManager.AskInfo(query.dataset))
      dataManager.reply(Some(sourceInfo))

      dataManager.expectMsg(DataStoreManager.AskInfoAndViews(query.dataset))
      dataManager.reply(Seq(sourceInfo))

      when(mockPlanner.requestViewForQuery(query, sourceInfo, Seq.empty)).thenReturn(false)

      sender.expectMsg(JsArray(Seq(JsBoolean(false))))

      ok
    }

    // A test to send a request with unknown dataset to client
    // The expected result in this test is noSuchDatasetJson
    "send the NoSuchData msg if the request is on a unknown dataset" in {
      val sender = new TestProbe(system)
      val dataManager = new TestProbe(system)
      val mockParser = mock[JSONParser]
      val mockPlanner = mock[QueryPlanner]

      val jsonRequest = JsObject(Seq("fake" -> JsNumber(1)))
      val query = Query(sourceInfo.name)
      when(mockParser.parse(jsonRequest, twitterSchemaMap)).thenReturn((Seq(query), QueryExeOption.NoSliceNoContinue))
      when(mockParser.getDatasets(jsonRequest)).thenReturn(Set(sourceInfo.name))


      val client = system.actorOf(ViewStatusClient.props(mockParser, dataManager.ref, mockPlanner, Config.Default, sender.ref))
      sender.send(client, jsonRequest)
      dataManager.expectMsg(DataStoreManager.AskInfo(query.dataset))
      dataManager.reply(None)

      sender.expectMsg(ViewStatusClient.noSuchDatasetJson(sourceInfo.name))
      ok
    }
  }
}
