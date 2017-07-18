package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.impl.TestQuery._
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.{DateTime, DateTimeZone, Interval}
import play.api.libs.json.Json

object TestDataSetInfo {

  DateTimeZone.setDefault(DateTimeZone.UTC)
  val starDateTime = new DateTime(2004, 12, 25, 0, 0, 0, 0)
  val endDateTime = new DateTime(2016, 1, 1, 0, 0, 0, 0)
  val endTimeString = "2016-01-01T00:00:00.000Z"
  val startTimeString = "2004-12-25T00:00:00.000Z"

  val interval = new Interval(starDateTime, endDateTime)
  val globalAggr = GlobalAggregateStatement(aggrCount)
  val filter = Seq(stateFilter, timeFilter, textFilter)
  val group = GroupStatement(Seq(byState, byHour), Seq(aggrCount))
  val groupByTag = GroupStatement(Seq(byTag), Seq(aggrCount))
  val groupByBin = GroupStatement(Seq(byBin), Seq(aggrCount))

  val createQuery = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
  val berryAggrByTagQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq.empty, Some(group), None)
  val unnestQuery = new Query(TwitterDataSet, Seq.empty, Seq.empty, filter, Seq(unnestHashTag), Some(groupByTag), Some(selectTop10Tag))
  val groupByBinQuery = new Query(TwitterDataSet, groups = Some(groupByBin))


  val simpleDataSetInfo = new DataSetInfo("twitter.ds_tweet", None, Schema("tweet", Seq(createAt), Seq.empty, Seq.empty, createAt), interval, new Stats(endDateTime, endDateTime, endDateTime, 0))
  val fieldsDataSetInfo = new DataSetInfo("twitter.ds_tweet", None, Schema("tweet", Seq(NumberField("id"), StringField("name"), createAt), Seq.empty, Seq.empty, createAt), interval, new Stats(endDateTime, endDateTime, endDateTime, 0))
  val queryDataSetInfo = new DataSetInfo("twitter.ds_tweet", Some(createQuery), Schema("tweet", Seq(createAt), Seq.empty, Seq.empty, createAt), interval, new Stats(endDateTime, endDateTime, endDateTime, 0))
  val berryAggrByTagViewDataSetInfo = new DataSetInfo("twitter.ds_tweet", Some(berryAggrByTagQuery), Schema("tweet", Seq(createAt), Seq.empty, Seq.empty, createAt), interval, new Stats(endDateTime, endDateTime, endDateTime, 0))
  val unnestQueryDataSetInfo = new DataSetInfo("twitter.ds_tweet", Some(unnestQuery), Schema("tweet", Seq(createAt), Seq.empty, Seq.empty, createAt), interval, new Stats(endDateTime, endDateTime, endDateTime, 0))
  val byBinDataSetInfo = new DataSetInfo("twitter.ds_tweet", Some(groupByBinQuery), Schema("tweet", Seq(createAt), Seq.empty, Seq.empty, createAt), interval, new Stats(endDateTime, endDateTime, endDateTime, 0))


  val simpleDataSetInfoJSON = Json.parse(
    s"""
       |{
       | "name": "twitter.ds_tweet",
       | "schema": {
       |		"typeName": "tweet",
       |     "dimension": [{
       |      "name": "create_at",
       |      "isOptional": false,
       |      "datatype": "Time"}
       |      ],
       |     "measurement": [],
       |     "primaryKey": [],
       |     "timeField": "create_at"
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
       |      "datatype": "String"},
       |      {
       |      "name": "create_at",
       |      "isOptional": false,
       |      "datatype": "Time"}
       |     ],
       |     "measurement": [],
       |     "primaryKey": [],
       |     "timeField": "create_at"
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
       |     "dimension": [{
       |      "name": "create_at",
       |      "isOptional": false,
       |      "datatype": "Time"}
       |      ],
       |     "measurement": [],
       |     "primaryKey": [],
       |     "timeField": "create_at"
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

  val BinDataSetInfoJSON = Json.parse(
    s"""
       |{
       | "name": "twitter.ds_tweet",
       | "createQuery":
       |$groupByBinJSON,
       | "schema": {
       |		"typeName": "tweet",
       |     "dimension": [{
       |      "name": "create_at",
       |      "isOptional": false,
       |      "datatype": "Time"}
       |      ],
       |     "measurement": [],
       |     "primaryKey": [],
       |     "timeField": "create_at"
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
       |     "dimension": [{
       |      "name": "create_at",
       |      "isOptional": false,
       |      "datatype": "Time"}
       |      ],
       |     "measurement": [],
       |     "primaryKey": [],
       |     "timeField": "create_at"
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
       |     "dimension": [{
       |      "name": "create_at",
       |      "isOptional": false,
       |      "datatype": "Time"}
       |      ],
       |     "measurement": [],
       |     "primaryKey": [],
       |     "timeField": "create_at"
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

  val sourceDataSetInfoJSON = Json.parse(
    s"""
       |{
       |"name":"twitter.ds_tweet",
       |"schema":{
       |  "typeName":"twitter.typeTweet",
       |   "dimension":[{"name":"create_at","isOptional":false,"datatype":"Time"},
       |      {"name":"id","isOptional":false,"datatype":"Number"},
       |      {"name":"coordinate","isOptional":false,"datatype":"Point"},
       |      {"name":"lang","isOptional":false,"datatype":"String"},
       |      {"name":"is_retweet","isOptional":false,"datatype":"Boolean"},
       |      {"name":"hashtags","isOptional":true,"datatype":"Bag","innerType":"String"},
       |      {"name":"user_mentions","isOptional":true,"datatype":"Bag","innerType":"Number"},
       |      {"name":"user.id","isOptional":false,"datatype":"Number"},
       |      {"name":"geo_tag.stateID","isOptional":false,"datatype":"Number"},
       |      {"name":"geo_tag.countyID","isOptional":false,"datatype":"Number"},
       |      {"name":"geo_tag.cityID","isOptional":false,"datatype":"Number"},
       |      {"name":"geo","isOptional":false,"datatype":"Hierarchy","innerType":"Number",
       |          "levels":[{"level":"state","field":"geo_tag.stateID"},
       |          {"level":"county","field":"geo_tag.countyID"},
       |          {"level":"city","field":"geo_tag.cityID"}]}],
       |   "measurement":[{"name":"text","isOptional":false,"datatype":"Text"},
       |      {"name":"in_reply_to_status","isOptional":false,"datatype":"Number"},
       |      {"name":"in_reply_to_user","isOptional":false,"datatype":"Number"},
       |      {"name":"favorite_count","isOptional":false,"datatype":"Number"},
       |      {"name":"retweet_count","isOptional":false,"datatype":"Number"},
       |      {"name":"user.status_count","isOptional":false,"datatype":"Number"}],
       |   "primaryKey":["id"],"timeField":"create_at"},
       |"dataInterval":{"start":"2015-01-01T00:00:00.000Z",
       |                "end":"2017-01-01T00:00:00.000Z"},
       |"stats":{"createTime":"2015-01-01T00:00:00.000Z",
       |         "lastModifyTime":"2017-01-01T00:00:00.000Z",
       |         "lastReadTime":"2017-01-01T00:00:00.000Z",
       |         "cardinality":10000
       |         }
       |}
       """.stripMargin)

  val zikaDataSetInfoJSON = Json.parse(
    s"""
       |{
       |"name":"zika",
       |"createQuery":
       |$zikaJSON,
       |"schema":{
       |  "typeName":"twitter.typeTweet",
       |   "dimension":[{"name":"create_at","isOptional":false,"datatype":"Time"},
       |      {"name":"id","isOptional":false,"datatype":"Number"},
       |      {"name":"coordinate","isOptional":false,"datatype":"Point"},
       |      {"name":"lang","isOptional":false,"datatype":"String"},
       |      {"name":"is_retweet","isOptional":false,"datatype":"Boolean"},
       |      {"name":"hashtags","isOptional":true,"datatype":"Bag","innerType":"String"},
       |      {"name":"user_mentions","isOptional":true,"datatype":"Bag","innerType":"Number"},
       |      {"name":"user.id","isOptional":false,"datatype":"Number"},
       |      {"name":"geo_tag.stateID","isOptional":false,"datatype":"Number"},
       |      {"name":"geo_tag.countyID","isOptional":false,"datatype":"Number"},
       |      {"name":"geo_tag.cityID","isOptional":false,"datatype":"Number"},
       |      {"name":"geo","isOptional":false,"datatype":"Hierarchy","innerType":"Number",
       |          "levels":[{"level":"state","field":"geo_tag.stateID"},
       |          {"level":"county","field":"geo_tag.countyID"},
       |          {"level":"city","field":"geo_tag.cityID"}]}],
       |   "measurement":[{"name":"text","isOptional":false,"datatype":"Text"},
       |      {"name":"in_reply_to_status","isOptional":false,"datatype":"Number"},
       |      {"name":"in_reply_to_user","isOptional":false,"datatype":"Number"},
       |      {"name":"favorite_count","isOptional":false,"datatype":"Number"},
       |      {"name":"retweet_count","isOptional":false,"datatype":"Number"},
       |      {"name":"user.status_count","isOptional":false,"datatype":"Number"}],
       |   "primaryKey":["id"],"timeField":"create_at"},
       |"dataInterval":{"start":"2015-01-01T00:00:00.000Z",
       |                "end":"2016-06-01T00:00:00.000Z"},
       |"stats":{"createTime":"2015-01-01T00:00:00.000Z",
       |         "lastModifyTime":"2016-06-01T00:00:00.000Z",
       |         "lastReadTime":"2016-06-01T00:00:00.000Z",
       |         "cardinality":50
       |         }
       |}
       """.stripMargin)


}
