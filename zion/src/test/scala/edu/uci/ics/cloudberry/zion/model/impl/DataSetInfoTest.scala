package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema.Schema
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
           |		"typeName": "zika",
           |     "dimension": [],
           |     "measurement": [],
           |     "primaryKey": [],
           |     "timeField": ""
           | },
           | "dataInterval": {"start":"2004-12-25",
           |                  "end":"2016-01-01"},
           | "stats": { "createTime": "2016-01-01",
           |            "lastModifyTime": "2016-01-01",
           |            "lastReadTime": "2016-01-01",
           |            "cardinality": 0
           |          }
           |}
    """.stripMargin)

      val start = new DateTime(2004, 12, 25, 0, 0, 0, 0)
      print(start.toString)
      val end = new DateTime(2016, 1, 1, 0, 0, 0, 0)
      val interval = new Interval(start, end);
      val actualQuery = parser.parse(dataSetInfoJSON)
      val expectQuery = new DataSetInfo("twitter.ds_tweet", None, Schema("zika", Seq.empty, Seq.empty, Seq.empty, ""), interval, new Stats(end, end, end, 0))
      actualQuery must_== expectQuery
    }
    "read dataSetInfo containing Schema fields" in {
      ok
    }
    "read dataSetInfo containing a createQuery" in {
      ok
    }
    "write a sample dataSetInfo" in {
      ok
    }
    "write dataSetInfo containing a createQuery" in {
      ok
    }
  }
}
