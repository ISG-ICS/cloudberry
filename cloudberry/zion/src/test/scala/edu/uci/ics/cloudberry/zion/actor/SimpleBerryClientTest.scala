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

class SimpleBerryClientTest extends TestkitExample with SpecificationLike with MockConnClient {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  import edu.uci.ics.cloudberry.zion.model.impl.TestQuery._
  import org.mockito.Mockito._

  sequential

  "Client" should {
    "send multiple queries to dataManager if the planner said so" in {
      val sender = new TestProbe(system)
      val dataManager = new TestProbe(system)
      val mockParser = mock[JSONParser]
      val mockPlanner = mock[QueryPlanner]

      val jsonRequest = JsObject(Seq("fake" -> JsNumber(1)))
      val query = Query(sourceInfo.name)
      when(mockParser.parse(jsonRequest, twitterSchemaMap)).thenReturn((Seq(query), QueryExeOption.NoSliceNoContinue))
      when(mockParser.getDatasets(jsonRequest)).thenReturn(Set(TwitterDataSet))

      val client = system.actorOf(BerryClient.props(mockParser, dataManager.ref, mockPlanner, Config.Default, sender.ref))
      sender.send(client, jsonRequest)

      dataManager.expectMsg(DataStoreManager.AskInfo(query.dataset))
      dataManager.reply(Some(sourceInfo))

      dataManager.expectMsg(DataStoreManager.AskInfoAndViews(query.dataset))
      dataManager.reply(Seq(sourceInfo))

      val query1 = Query(sourceInfo.name, filter = Seq(textFilter))
      val query2 = Query(sourceInfo.name, filter = Seq(timeFilter))
      when(mockPlanner.makePlan(query, sourceInfo, Seq.empty)).thenReturn((Seq(query1, query2), QueryPlanner.Unioner))

      val create = CreateView("zika", zikaCreateQuery)
      when(mockPlanner.suggestNewView(query, sourceInfo, Seq.empty)).thenReturn(Seq(create))

      val json1 = JsArray(Seq(Json.obj("a" -> 4)))
      val json2 = JsArray(Seq(Json.obj("b" -> 8)))

      dataManager.expectMsg(query1)
      dataManager.reply(json1)
      dataManager.expectMsg(query2)
      dataManager.reply(json2)

      sender.expectMsg(JsArray(Seq(JsArray(Seq(Json.obj("a" -> 4), Json.obj("b" -> 8))))))

      dataManager.expectMsg(DataStoreManager.AskInfoAndViews(query.dataset))
      dataManager.reply(Seq(sourceInfo))
      dataManager.expectMsg(create)
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


      val client = system.actorOf(BerryClient.props(mockParser, dataManager.ref, mockPlanner, Config.Default, sender.ref))
      sender.send(client, jsonRequest)
      dataManager.expectMsg(DataStoreManager.AskInfo(query.dataset))
      dataManager.reply(None)

      sender.expectMsg(BerryClient.noSuchDatasetJson(sourceInfo.name))
      ok
    }
  }
}
