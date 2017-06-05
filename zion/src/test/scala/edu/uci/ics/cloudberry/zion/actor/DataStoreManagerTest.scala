package edu.uci.ics.cloudberry.zion.actor

import java.util.concurrent.Executors

import akka.actor._
import akka.testkit.TestProbe
import edu.uci.ics.cloudberry.zion.actor.DataStoreManager._
import edu.uci.ics.cloudberry.zion.actor.OriginalDataAgent.NewStats
import edu.uci.ics.cloudberry.zion.common.Config
import edu.uci.ics.cloudberry.zion.model.datastore.{CollectStatsException, IDataConn, IQLGenerator, IQLGeneratorFactory}
import edu.uci.ics.cloudberry.zion.model.impl._
import edu.uci.ics.cloudberry.zion.model.schema.TimeField.TimeFormat
import edu.uci.ics.cloudberry.zion.model.schema._
import edu.uci.ics.cloudberry.zion.model.util.MockConnClient
import org.joda.time.{DateTime, Interval}
import org.specs2.mutable.SpecificationLike
import play.api.libs.json.{JsArray, JsSuccess, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class DataStoreManagerTest extends TestkitExample with SpecificationLike with MockConnClient {

  implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(2))

  sequential

  import edu.uci.ics.cloudberry.zion.model.impl.TestQuery._
  import org.mockito.Mockito._

  import scala.concurrent.duration._

  "DataManager" should {
    val sender = new TestProbe(system)
    val view = new TestProbe(system)
    val base = new TestProbe(system)
    val meta = new TestProbe(system)
    val metaDataSet = "metaDataSet"

    def testActorMaker(agentType: AgentType.Value,
                       context: ActorRefFactory,
                       actorName: String,
                       dbName: String,
                       dbSchema: AbstractSchema,
                       dataSetInfoOpt: Option[DataSetInfo],
                       qLGenerator: IQLGenerator,
                       conn: IDataConn,
                       appConfig: Config
                      )(implicit ec: ExecutionContext): ActorRef = {
      import AgentType._
      agentType match {
        case Meta => meta.ref
        case Origin => base.ref
        case View => view.ref
      }
    }

    "load the meta info when preStart" in {
      val mockParserFactory = mock[IQLGeneratorFactory]
      val mockConn = mock[IDataConn]

      val initialInfo = JsArray(Seq(DataSetInfo.write(sourceInfo)))
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

      val initialInfo = JsArray(Seq(DataSetInfo.write(sourceInfo)))
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

      val initialInfo = JsArray(Seq(DataSetInfo.write(sourceInfo)))
      val dataManager = system.actorOf(Props(new DataStoreManager(metaDataSet, mockConn, mockParserFactory, Config.Default, testActorMaker)))
      val metaQuery = meta.receiveOne(5 seconds)
      metaQuery.asInstanceOf[Query].dataset must_== metaDataSet
      meta.reply(initialInfo)

      val query = Query(dataset = sourceInfo.name)
      sender.send(dataManager, query)
      base.expectMsg(query)
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

      val initialInfo = JsArray(Seq(DataSetInfo.write(sourceInfo)))
      val dataManager = system.actorOf(Props(new DataStoreManager(metaDataSet, mockConn, mockParserFactory, Config.Default, testActorMaker)))
      val metaQuery = meta.receiveOne(5 seconds)
      metaQuery.asInstanceOf[Query].dataset must_== metaDataSet
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
      viewInfo.stats.lastModifyTime.getMillis must be_>=(now.getMillis)
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

      val initialInfo = Json.toJson(Seq(DataSetInfo.write(sourceInfo), DataSetInfo.write(zikaHalfYearViewInfo))).asInstanceOf[JsArray]
      val dataManager = system.actorOf(Props(new DataStoreManager(metaDataSet, mockConn, mockParserFactory, Config.Default, testActorMaker)))
      val metaQuery = meta.receiveOne(5 seconds)
      metaQuery.asInstanceOf[Query].dataset must_== metaDataSet
      meta.reply(initialInfo)

      sender.send(dataManager, DataStoreManager.AreYouReady)
      sender.expectMsg(true)
      sender.send(dataManager, DataStoreManager.AskInfoAndViews(sourceInfo.name))
      sender.expectMsg(Seq(sourceInfo, zikaHalfYearViewInfo))

      val appendView = AppendView(zikaHalfYearViewInfo.name, Query(sourceInfo.name))
      sender.send(dataManager, appendView)
      view.expectMsg(appendView)
      view.reply(true)
      sender.expectNoMsg(1 seconds)
      meta.receiveOne(1 seconds)

      sender.send(dataManager, DataStoreManager.AskInfoAndViews(zikaHalfYearViewInfo.name))
      val newInfo = sender.receiveOne(1 second).asInstanceOf[Seq[DataSetInfo]].head
      newInfo.name must_== zikaHalfYearViewInfo.name
      newInfo.dataInterval.getEnd must_== TimeField.TimeFormat.parseDateTime((viewStatJson \\ "max").head.as[String])
      newInfo.stats.cardinality must_== (viewStatJson \\ "count").head.as[Long]
      newInfo.stats.lastModifyTime.getMillis must be_>=(now.getMillis)
    }
    "use existing child to solve the query" in {
      ok
    }
    "receive NewStats and update Stats in meta" in {
      val mockParserFactory = mock[IQLGeneratorFactory]
      val mockConn = mock[IDataConn]

      val initialInfo = JsArray(Seq(DataSetInfo.write(sourceInfo)))
      val dataManager = system.actorOf(Props(new DataStoreManager(metaDataSet, mockConn, mockParserFactory, Config.Default, testActorMaker)))
      val metaQuery = meta.receiveOne(5 seconds)
      metaQuery.asInstanceOf[Query].dataset must_== metaDataSet
      meta.reply(initialInfo)

      val newStats = NewStats(sourceInfo.name, 999)
      sender.send(dataManager, newStats)
      meta.receiveOne(1 second)

      sender.send(dataManager, AskInfoAndViews(sourceInfo.name))
      val updatedInfo = sender.receiveOne(5 second).asInstanceOf[Seq[DataSetInfo]].head
      updatedInfo.stats.cardinality must_== sourceStat.cardinality + newStats.additionalCount

      ok
    }
  }

  "Data schema registering process" should {
    val sender = new TestProbe(system)
    val view = new TestProbe(system)
    val base = new TestProbe(system)
    val meta = new TestProbe(system)
    val metaDataSet = "metaDataSet"

    def testActorMaker(agentType: AgentType.Value,
                       context: ActorRefFactory,
                       actorName: String,
                       dbName: String,
                       dbSchema: AbstractSchema,
                       dataSetInfoOpt: Option[DataSetInfo],
                       qLGenerator: IQLGenerator,
                       conn: IDataConn,
                       appConfig: Config
                      )(implicit ec: ExecutionContext): ActorRef = {
      import AgentType._
      agentType match {
        case Meta => meta.ref
        case Origin => base.ref
        case View => view.ref
      }
    }

    val parser = new AQLGenerator
    val mockParserFactory = mock[IQLGeneratorFactory]
    when(mockParserFactory.apply()).thenReturn(parser)
    val mockConn = mock[IDataConn]

    val initialInfo = JsArray(Seq(DataSetInfo.write(sourceInfo)))
    val dataManager = system.actorOf(Props(new DataStoreManager(metaDataSet, mockConn, mockParserFactory, Config.Default, testActorMaker)))
    meta.receiveOne(1 second)
    meta.reply(initialInfo)
    sender.send(dataManager, DataStoreManager.AreYouReady)
    sender.expectMsg(true)

    val field1 = TimeField("myTime")
    val field2 = TextField("myText")
    val field3 = StringField("myString")
    val field4 = NumberField("myNumber")

    val temporalSchema = UnresolvedSchema("temporalType", Seq(field1, field2), Seq(field3, field4), Seq("myString"), Some("myTime"))
    val registerTemporal = Register("temporal", temporalSchema)

    val staticSchema = UnresolvedSchema("staticType", Seq(field1, field2), Seq(field3, field4), Seq("myString"), None)
    val registerStatic = Register("static", staticSchema)

    "parse json register request" in {
      val temporalRegisterJson = Json.parse(
        """
          |{
          |  "dataset": "temporal",
          |  "schema": {
          |    "typeName": "temporalType",
          |    "dimension": [
          |      {"name":"myTime","isOptional":false,"datatype":"Time"},
          |      {"name":"myText","isOptional":false,"datatype":"Text"}
          |    ],
          |    "measurement": [
          |      {"name":"myString","isOptional":false,"datatype":"String"},
          |      {"name":"myNumber","isOptional":false,"datatype":"Number"}
          |    ],
          |    "primaryKey": ["myString"],
          |    "timeField": "myTime"
          |  }
          |}
        """.stripMargin)

      temporalRegisterJson.validate[Register] match {
        case jsonResult: JsSuccess[Register] => jsonResult.get mustEqual registerTemporal
        case _ => throw new IllegalArgumentException
      }

      val staticRegisterJson = Json.parse(
        """
          |{
          |  "dataset": "static",
          |  "schema": {
          |    "typeName": "staticType",
          |    "dimension": [
          |      {"name":"myTime","isOptional":false,"datatype":"Time"},
          |      {"name":"myText","isOptional":false,"datatype":"Text"}
          |    ],
          |    "measurement": [
          |      {"name":"myString","isOptional":false,"datatype":"String"},
          |      {"name":"myNumber","isOptional":false,"datatype":"Number"}
          |    ],
          |    "primaryKey": ["myString"]
          |  }
          |}
        """.stripMargin)

      staticRegisterJson.validate[Register] match {
        case jsonResult: JsSuccess[Register] => jsonResult.get mustEqual registerStatic
        case _ => throw new IllegalArgumentException
      }

      ok
    }
    "respond success if register temporal dataset and registered dataset can be successfully retrieved with correct stats information" in {
      val statJson = JsArray(Seq(Json.obj(
        "min" -> "2015-01-01T00:00:00.000Z",
        "max" -> "2016-01-01T00:00:00.000Z",
        "count" -> 2000
      )))
      when(mockConn.postQuery(any[String])).thenReturn(Future(statJson))

      sender.send(dataManager, registerTemporal)
      sender.expectMsg(DataManagerResponse(true, "Register Finished: temporal dataset " + registerTemporal.dataset + " has successfully registered.\n"))
      meta.receiveOne(1 second)

      sender.send(dataManager, AskInfoAndViews(registerTemporal.dataset))
      val infos = sender.receiveOne(1 second).asInstanceOf[List[DataSetInfo]]
      infos.map { dataset: DataSetInfo =>
        dataset.name must_== registerTemporal.dataset
        dataset.createQueryOpt must_== None
        dataset.schema must_== registerTemporal.schema.toResolved

        val minTime = "2015-01-01T00:00:00.000Z"
        val maxTime = "2016-01-01T00:00:00.000Z"
        val interval = new Interval(TimeFormat.parseDateTime(minTime), TimeFormat.parseDateTime(maxTime))
        dataset.dataInterval must_== interval

        dataset.stats.cardinality must_== 2000.asInstanceOf[Long]
      }
      ok
    }
    "respond success if register lookup dataset and registered dataset can be successfully retrieved with designated stats information" in {
      sender.send(dataManager, registerStatic)
      sender.expectMsg(DataManagerResponse(true, "Register Finished: lookup dataset " + registerStatic.dataset + " has successfully registered.\n"))
      meta.receiveOne(1 second)

      sender.send(dataManager, AskInfoAndViews(registerStatic.dataset))
      val infos = sender.receiveOne(1 second).asInstanceOf[List[DataSetInfo]]
      infos.map { dataset: DataSetInfo =>
        dataset.name must_== registerStatic.dataset
        dataset.createQueryOpt must_== None
        dataset.schema must_== registerStatic.schema.toResolved
        dataset.dataInterval.getStart must_== new DateTime(1970, 1, 1, 0, 0, 0, 0)
        dataset.stats.cardinality must_== 1000.asInstanceOf[Long]
      }
      ok
    }
    "respond failure if register an existing dataset" in {
      sender.send(dataManager, registerTemporal)
      sender.expectMsg(DataManagerResponse(false, "Register Denied: dataset " + registerTemporal.dataset + " already existed.\n"))
      ok
    }
    "respond failure if register temporal dataset where time field cannot be found in dimensions and measurements" in {
      val schemaFalseTimeField = UnresolvedSchema("typeFalseTimeField", Seq(field1, field2), Seq(field3, field4), Seq("myString"), Some("falseTimeField"))
      val registerRequestFalseTimeField = Register("TableFalseTimeField", schemaFalseTimeField)
      sender.send(dataManager, registerRequestFalseTimeField)
      sender.expectMsg(DataManagerResponse(false, "Register Denied. Field Not Found Error: " + schemaFalseTimeField.timeField.get + " is not found in dimensions and measurements: not a valid field.\n"))
      ok
    }
    "respond failure if register temporal dataset where time field is not a valid type of timeField" in {
      val schemaNotATimeField = UnresolvedSchema("typeNotATimeField", Seq(field1, field2), Seq(field3, field4), Seq("myString"), Some("myNumber"))
      val registerRequestNotATimeField = Register("TableNotATimeField", schemaNotATimeField)
      sender.send(dataManager, registerRequestNotATimeField)
      sender.expectMsg(DataManagerResponse(false, "Register Denied. Field Parsing Error: " + "Specified timeField " + schemaNotATimeField.timeField.get + " of schema " + schemaNotATimeField.typeName + " is not in TimeField format."))
      ok
    }
    "respond failure if register temporal dataset when collecting stats fails" in {
      val failureMsg = "FAIL"
      when(mockConn.postQuery(any[String])).thenThrow(new CollectStatsException(failureMsg))

      val schemaCollectFail = UnresolvedSchema("CollectFailure", Seq(field1, field2), Seq(field3, field4), Seq("myString"), Some("myTime"))
      val registerRequestCollectFail = Register("CollectFailure", schemaCollectFail)
      sender.send(dataManager, registerRequestCollectFail)
      sender.expectMsg(DataManagerResponse(false, "Register Denied. Collect Stats Error: " + failureMsg))
      ok
    }
  }

  "Data schema deregistering process" should {
    val sender = new TestProbe(system)
    val view = new TestProbe(system)
    val base = new TestProbe(system)
    val meta = new TestProbe(system)
    val metaDataSet = "metaDataSet"

    def createDelegateActor(name: String, context: ActorRefFactory, actor: ActorRef) : ActorRef = {
      context.actorOf(Props(new Actor{
        override def receive: Receive = {
          case any => actor.forward(any)
        }

        override def postStop(): Unit = {
          actor ! "PoisonPill"
        }
      }), name)
    }

    def testActorMaker(agentType: AgentType.Value,
                       context: ActorRefFactory,
                       actorName: String,
                       dbName: String,
                       dbSchema: AbstractSchema,
                       dataSetInfoOpt: Option[DataSetInfo],
                       qLGenerator: IQLGenerator,
                       conn: IDataConn,
                       appConfig: Config
                      )(implicit ec: ExecutionContext): ActorRef = {
      import AgentType._
      agentType match {
        case Meta => createDelegateActor(actorName, context, meta.ref)
        case Origin => createDelegateActor(actorName, context, base.ref)
        case View => createDelegateActor(actorName, context, view.ref)
      }
    }

    val mockParserFactory = mock[IQLGeneratorFactory]
    val mockConn = mock[IDataConn]
    val initialInfo = Json.toJson(Seq(DataSetInfo.write(sourceInfo), DataSetInfo.write(zikaHalfYearViewInfo))).asInstanceOf[JsArray]
    val dataManager = system.actorOf(Props(new DataStoreManager(metaDataSet, mockConn, mockParserFactory, Config.Default, testActorMaker)))
    meta.receiveOne(1 second)
    meta.reply(initialInfo)
    sender.send(dataManager, DataStoreManager.AreYouReady)
    sender.expectMsg(true)
    sender.send(dataManager, DataStoreManager.AskInfoAndViews(sourceInfo.name))
    sender.expectMsg(Seq(sourceInfo, zikaHalfYearViewInfo))

    val deregisterRequest = Deregister(sourceInfo.name)

    "parse deregister request" in {
      val jsonDeregisterRequest = Json.parse(
        s"""
          |{
          |   "dataset": "${sourceInfo.name}"
          |}
        """.stripMargin)

      jsonDeregisterRequest.validate[Deregister] match {
        case jsonResult: JsSuccess[Deregister] => jsonResult.get mustEqual deregisterRequest
        case _ => throw new IllegalArgumentException
      }
      ok
    }
    "If deregister an existing data model, remove from metaData, delete meta dataset record, drop views, kill dataset and views actor and respond success" in {
      val query = Query(sourceInfo.name)
      sender.send(dataManager, query)
      base.expectMsg(query)
      val appendView = AppendView(zikaHalfYearViewInfo.name, Query(sourceInfo.name))
      sender.send(dataManager, appendView)
      view.expectMsg(appendView)

      sender.send(dataManager, deregisterRequest)
      sender.expectMsg(DataManagerResponse(true, "Deregister Finished: dataset " + deregisterRequest.dataset + " has successfully removed.\n"))
      val datasetFilter = FilterStatement(DataSetInfo.MetaSchema.fieldMap("name"), None, Relation.matches, Seq(deregisterRequest.dataset))
      meta.expectMsg(DeleteRecord(metaDataSet, Seq(datasetFilter)))
      meta.expectMsg(DropView(zikaHalfYearViewInfo.name))
      base.expectMsg("PoisonPill")
      view.expectMsg("PoisonPill")

      sender.send(dataManager, AskInfoAndViews(deregisterRequest.dataset))
      sender.expectMsg(Seq.empty)
      ok
    }
    "If deregister a non-existing data model, respond failure" in {
      val anotherDeregisterRequest = Deregister("NoSuchDataSet")
      sender.send(dataManager, anotherDeregisterRequest)
      sender.expectMsg(DataManagerResponse(false, "Deregister Denied: dataset " + anotherDeregisterRequest.dataset + " does not exist in database.\n"))
      ok
    }
  }
}
