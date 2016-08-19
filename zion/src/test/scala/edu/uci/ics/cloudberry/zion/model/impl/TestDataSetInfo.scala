package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.impl.TestQuery._
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.{DateTime, Interval}
import play.api.libs.json.Json

object TestDataSetInfo {

  val starDateTime = new DateTime(2004, 12, 25, 0, 0, 0, 0)
  val endDateTime = new DateTime(2016, 1, 1, 0, 0, 0, 0)
  val endTimeString = "2016-01-01T08:00:00.000Z"
  val startTimeString = "2004-12-25T08:00:00.000Z"

  val interval = new Interval(starDateTime, endDateTime)
  val fields = Seq(NumberField("id"), StringField("name"))
  val globalAggr = GlobalAggregateStatement(aggrCount)
  val filter = Seq(stateFilter, timeFilter, textFilter)
  val group = GroupStatement(Seq(byState, byHour), Seq(aggrCount))
  val groupByTag = GroupStatement(Seq(byTag), Seq(aggrCount))

  val createQuery = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
  val complexQuery = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
  val unnestQuery = new Query(TwitterDataSet, Seq.empty, filter, Seq(unnestHashTag), Some(groupByTag), Some(selectTop10Tag))


  val simpleDataSetInfo = new DataSetInfo("twitter.ds_tweet", None, Schema("tweet", Seq.empty, Seq.empty, Seq.empty, ""), interval, new Stats(endDateTime, endDateTime, endDateTime, 0))
  val fieldsDataSetInfo = new DataSetInfo("twitter.ds_tweet", None, Schema("tweet", fields, Seq.empty, Seq.empty, ""), interval, new Stats(endDateTime, endDateTime, endDateTime, 0))
  val queryDataSetInfo = new DataSetInfo("twitter.ds_tweet", Some(createQuery), Schema("tweet", Seq.empty, Seq.empty, Seq.empty, ""), interval, new Stats(endDateTime, endDateTime, endDateTime, 0))
  val complexQueryDataSetInfo = new DataSetInfo("twitter.ds_tweet", Some(complexQuery), Schema("tweet", Seq.empty, Seq.empty, Seq.empty, ""), interval, new Stats(endDateTime, endDateTime, endDateTime, 0))
  val unnestQueryDataSetInfo = new DataSetInfo("twitter.ds_tweet", Some(unnestQuery), Schema("tweet", Seq.empty, Seq.empty, Seq.empty, ""), interval, new Stats(endDateTime, endDateTime, endDateTime, 0))


  val simpleDataSetInfoJSON = Json.parse(
    s"""
       |{
       | "name": "twitter.ds_tweet",
       | "schema": {
       |		"typeName": "tweet",
       |     "dimension": [],
       |     "measurement": [],
       |     "primaryKey": [],
       |     "timeField": ""
       | },
       | "dataInterval": {"start":"$startTimeString",
       |                  "end":"$endTimeString"},
       | "stats": { "createTime": "$endTimeString",
       |            "lastModifyTime": "$endTimeString",
       |            "lastReadTime": "$endTimeString",
       |            "cardinality": 0
       |          }
       |}
    """.stripMargin)

  val fieldsDataSetInfoJSON = Json.parse(
    s"""
       |{
       | "name": "twitter.ds_tweet",
       | "schema": {
       |		"typeName": "tweet",
       |     "dimension": [{
       |      "name": "id",
       |      "isOptional": false,
       |      "datatype": "Number"},
       |      {
       |      "name": "name",
       |      "isOptional": false,
       |      "datatype": "String"}
       |     ],
       |     "measurement": [],
       |     "primaryKey": [],
       |     "timeField": ""
       | },
       | "dataInterval": {"start":"$startTimeString",
       |                  "end":"$endTimeString"},
       | "stats": { "createTime": "$endTimeString",
       |            "lastModifyTime": "$endTimeString",
       |            "lastReadTime": "$endTimeString",
       |            "cardinality": 0
       |          }
       |}
    """.stripMargin)

  val queryDataSetInfoJSON = Json.parse(
    s"""
       |{
       | "name": "twitter.ds_tweet",
       | "createQuery":
       |$globalCountJSON,
       | "schema": {
       |		"typeName": "tweet",
       |     "dimension": [],
       |     "measurement": [],
       |     "primaryKey": [],
       |     "timeField": ""
       | },
       | "dataInterval": {"start":"$startTimeString",
       |                  "end":"$endTimeString"},
       | "stats": { "createTime": "$endTimeString",
       |            "lastModifyTime": "$endTimeString",
       |            "lastReadTime": "$endTimeString",
       |            "cardinality": 0
       |          }
       |}
    """.stripMargin)

  val complexQueryDataSetInfoJSON = Json.parse(
    s"""
       |{
       | "name": "twitter.ds_tweet",
       | "createQuery":
       |$filterSelectJSON,
       | "schema": {
       |		"typeName": "tweet",
       |     "dimension": [],
       |     "measurement": [],
       |     "primaryKey": [],
       |     "timeField": ""
       | },
       | "dataInterval": {"start":"$startTimeString",
       |                  "end":"$endTimeString"},
       | "stats": { "createTime": "$endTimeString",
       |            "lastModifyTime": "$endTimeString",
       |            "lastReadTime": "$endTimeString",
       |            "cardinality": 0
       |          }
       |}
    """.stripMargin)
  val unnestQueryDataSetInfoJSON = Json.parse(
    s"""
       |{
       | "name": "twitter.ds_tweet",
       | "createQuery":
       |$topKHashTagJSON,
       | "schema": {
       |		"typeName": "tweet",
       |     "dimension": [],
       |     "measurement": [],
       |     "primaryKey": [],
       |     "timeField": ""
       | },
       | "dataInterval": {"start":"$startTimeString",
       |                  "end":"$endTimeString"},
       | "stats": { "createTime": "$endTimeString",
       |            "lastModifyTime": "$endTimeString",
       |            "lastReadTime": "$endTimeString",
       |            "cardinality": 0
       |          }
       |}
    """.stripMargin)

}