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
    "send query to dataManager to check whether it can be solved by view" in {
      val sender = new TestProbe(system)
      val dataManager = new TestProbe(system)
      val mockParser = mock[JSONParser]
      val mockPlanner = mock[QueryPlanner]

      val jsonRequest = JsObject(Seq("fake" -> JsNumber(1)))
      val query = Query(sourceInfo.name)
      val queryID = 1
      when(mockParser.parse(jsonRequest, twitterSchemaMap)).thenReturn((Seq(query), QueryExeOption.NoSliceNoContinue))
      when(mockParser.getDatasets(jsonRequest)).thenReturn(Set(TwitterDataSet))
      when(mockParser.getQueryID(jsonRequest)).thenReturn(queryID)

      val client = system.actorOf(ViewStatusClient.props(mockParser, dataManager.ref, mockPlanner, Config.Default, sender.ref))
      sender.send(client, jsonRequest)

      dataManager.expectMsg(DataStoreManager.AskInfo(query.dataset))
      dataManager.reply(Some(sourceInfo))

      dataManager.expectMsg(DataStoreManager.AskInfoAndViews(query.dataset))
      dataManager.reply(Seq(sourceInfo))

      when(mockPlanner.requestViewForQuery(query, sourceInfo, Seq.empty)).thenReturn(true)

      val resultArray = Json.arr(ViewStatusClient.resultJson(true))
      val resultArrayWithId= resultArray.append(JsObject(Seq("queryID" -> JsNumber(queryID))))

      sender.expectMsg(resultArrayWithId)

      ok
    }

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
