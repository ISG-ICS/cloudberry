package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.impl.TestQuery._
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.{DateTime, Interval}
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class DataSetInfoTest extends Specification {

  val parser = DataSetInfo

  "DataSetInfo" should {
    "read a sample dataSetInfo" in {
      val dataSetInfoJSON = Json.parse(
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
           | "dataInterval": {"start":"2004-12-25T00:00:00.000-08:00",
           |                  "end":"2016-01-01T00:00:00.000-08:00"},
           | "stats": { "createTime": "2016-01-01T00:00:00.000-08:00",
           |            "lastModifyTime": "2016-01-01T00:00:00.000-08:00",
           |            "lastReadTime": "2016-01-01T00:00:00.000-08:00",
           |            "cardinality": 0
           |          }
           |}
    """.stripMargin)

      val start = new DateTime(2004, 12, 25, 0, 0, 0, 0)
      val end = new DateTime(2016, 1, 1, 0, 0, 0, 0).toDateTime
      val interval = new Interval(start, end);
      val actualQuery = parser.parse(dataSetInfoJSON)
      val expectQuery = new DataSetInfo("twitter.ds_tweet", None, Schema("tweet", Seq.empty, Seq.empty, Seq.empty, ""), interval, new Stats(end, end, end, 0))
      actualQuery must_== expectQuery
    }
    "read dataSetInfo containing Schema fields" in {
      val dataSetInfoJSON = Json.parse(
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
           | "dataInterval": {"start":"2004-12-25T00:00:00.000-08:00",
           |                  "end":"2016-01-01T00:00:00.000-08:00"},
           | "stats": { "createTime": "2016-01-01T00:00:00.000-08:00",
           |            "lastModifyTime": "2016-01-01T00:00:00.000-08:00",
           |            "lastReadTime": "2016-01-01T00:00:00.000-08:00",
           |            "cardinality": 0
           |          }
           |}
    """.stripMargin)

      val start = new DateTime(2004, 12, 25, 0, 0, 0, 0)
      val end = new DateTime(2016, 1, 1, 0, 0, 0, 0)
      val interval = new Interval(start, end);
      val fields = Seq(NumberField("id"), StringField("name"))
      val actualQuery = parser.parse(dataSetInfoJSON)
      val expectQuery = new DataSetInfo("twitter.ds_tweet", None, Schema("tweet", fields, Seq.empty, Seq.empty, ""), interval, new Stats(end, end, end, 0))
      actualQuery must_== expectQuery
    }
    "read dataSetInfo containing a createQuery" in {
      val dataSetInfoJSON = Json.parse(
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
           | "dataInterval": {"start":"2004-12-25T00:00:00.000-08:00",
           |                  "end":"2016-01-01T00:00:00.000-08:00"},
           | "stats": { "createTime": "2016-01-01T00:00:00.000-08:00",
           |            "lastModifyTime": "2016-01-01T00:00:00.000-08:00",
           |            "lastReadTime": "2016-01-01T00:00:00.000-08:00",
           |            "cardinality": 0
           |          }
           |}
    """.stripMargin)

      val globalAggr = GlobalAggregateStatement(aggrCount)
      val createQuery = new Query(dataset = TwitterDataSet, globalAggr = Some(globalAggr))
      val start = new DateTime(2004, 12, 25, 0, 0, 0, 0)
      val end = new DateTime(2016, 1, 1, 0, 0, 0, 0)
      val interval = new Interval(start, end);
      val actualQuery = parser.parse(dataSetInfoJSON)
      val expectQuery = new DataSetInfo("twitter.ds_tweet", Some(createQuery), Schema("tweet", Seq.empty, Seq.empty, Seq.empty, ""), interval, new Stats(end, end, end, 0))
      actualQuery must_== expectQuery
    }
    "write a sample dataSetInfo" in {
      val dataSetInfoJSON = Json.parse(
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
           | "dataInterval": {"start":"2004-12-25T00:00:00.000-08:00",
           |                  "end":"2016-01-01T00:00:00.000-08:00"},
           | "stats": { "createTime": "2016-01-01T00:00:00.000-08:00",
           |            "lastModifyTime": "2016-01-01T00:00:00.000-08:00",
           |            "lastReadTime": "2016-01-01T00:00:00.000-08:00",
           |            "cardinality": 0
           |          }
           |}
    """.stripMargin)

      val start = new DateTime(2004, 12, 25, 0, 0, 0, 0)
      val end = new DateTime(2016, 1, 1, 0, 0, 0, 0)
      val interval = new Interval(start, end);
      val expectJSON = dataSetInfoJSON
      val actualJSON = parser.write(DataSetInfo("twitter.ds_tweet", None, Schema("tweet", Seq.empty, Seq.empty, Seq.empty, ""), interval, new Stats(end, end, end, 0)))
      actualJSON must_== expectJSON
    }
    "write dataSetInfo containing a createQuery" in {
      ok
    }
    "read dataSetInfo containing complex fields" in {
      ok
    }
    "write dataSetInfo containing complex fields" in {
      ok
    }
  }
}
