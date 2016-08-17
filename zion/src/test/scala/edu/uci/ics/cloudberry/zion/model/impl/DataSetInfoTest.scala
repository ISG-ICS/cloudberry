package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema.Schema
import org.joda.time.{DateTime, Interval}
import org.specs2.mutable.Specification
import play.api.libs.json.Json

class DataSetInfoTest extends Specification {

  val parser = DataSetInfo

  "DataSetInfo" should {
    "read a sample datasetinfo" in {
      val datasetinfoJSON = Json.parse(
        s"""
           |{
           | "name": "twitter.ds_tweet",
           |	"schema": {
           |		"typeName": "zika",
           |     "dimension": [],
           |     "measurement": [],
           |     "primaryKey": [],
           |     "timeField": ""
           |	 },
           |   "interval": {
           |
             |   },
           |	"stats": {
           | }
           |}
    """.stripMargin)

      val start = new DateTime(2004, 12, 25, 0, 0, 0, 0)
      val end = new DateTime(2005, 1, 1, 0, 0, 0, 0)
      val interval = new Interval(start, end);
      val actualQuery = parser.parse(datasetinfoJSON)
      val expectQuery = new DataSetInfo("twitter.ds_tweet", None, Schema("zika", Seq.empty, Seq.empty, Seq.empty, ""), interval, new Stats(DateTime.now(), DateTime.now(), DateTime.now(), 0))
      actualQuery must_== expectQuery
    }
  }
}
