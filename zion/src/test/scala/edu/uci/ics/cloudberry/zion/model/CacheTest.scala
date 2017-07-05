package edu.uci.ics.cloudberry.zion.model
import edu.uci.ics.cloudberry.zion.model.impl.TestQuery
import edu.uci.ics.cloudberry.zion.model.schema._
import org.specs2.mutable.Specification
import play.api.libs.json.JsValue
//import play.libs.Json
import play.api.libs.json.{JsNull,Json,JsString,JsValue}
/**
  * Created by vigne on 6/17/2017.
  */
class CacheTest extends  Specification{
  "CacheTest" should {
    "Insert a Query in cache and Retrieve the same query" in {
      val CacheStartTime = "2016-01-01T00:00:00.000Z"
      val CacheEndTime = "2016-12-01T00:00:00.000Z"
      val TimeFilter = FilterStatement(TestQuery.createAt, None, Relation.inRange, Seq(CacheStartTime, CacheEndTime))
      val Filter = Seq(TimeFilter)
      val CacheQuerySample = new Query(TestQuery.TwitterDataSet, Seq.empty, Seq.empty, Filter, Seq.empty, None)

      val result: JsValue = Json.obj(
        "name" -> "Watership Down",
        "location" -> Json.obj("lat" -> 51.235685, "long" -> -1.309197)
      )
      val cache : Cache = new Cache()
      cache.insert(CacheQuerySample, result) must_== true
      val QueryStartTime = "2016-01-01T00:00:00.000Z"
      val QueryEndTime = "2016-12-01T00:00:00.000Z"
      val QueryTimeFilter = FilterStatement(TestQuery.createAt, None, Relation.inRange, Seq(QueryStartTime, QueryEndTime))
      val QueryFilter = Seq(QueryTimeFilter)
      val QuerySample = new Query(TestQuery.TwitterDataSet, Seq.empty, Seq.empty,QueryFilter, Seq.empty, None)
      cache.get(QuerySample) must_== result
      ok
    }
    "Insert and Retrieve where Query StartTime and Cache StartTime is same" in {
      val CacheStartTime = "2016-01-01T00:00:00.000Z"
      val CacheEndTime = "2016-12-01T00:00:00.000Z"
      val TimeFilter = FilterStatement(TestQuery.createAt, None, Relation.inRange, Seq(CacheStartTime, CacheEndTime))
      val Filter = Seq(TimeFilter)
      val CacheQuerySample = new Query(TestQuery.TwitterDataSet, Seq.empty,Seq.empty, Filter, Seq.empty, None)

      val result: JsValue = Json.obj(
        "name" -> "Watership Down",
        "location" -> Json.obj("lat" -> 51.235685, "long" -> -1.309197)
      )
      val cache : Cache = new Cache()
      cache.insert(CacheQuerySample, result) must_== true
      val QueryStartTime = "2016-01-01T00:00:00.000Z"
      val QueryEndTime = "2016-12-11T00:00:00.000Z"
      val QueryTimeFilter = FilterStatement(TestQuery.createAt, None, Relation.inRange, Seq(QueryStartTime, QueryEndTime))
      val QueryFilter = Seq(QueryTimeFilter)
      val QuerySample = new Query(TestQuery.TwitterDataSet, Seq.empty, Seq.empty,QueryFilter, Seq.empty, None)
      cache.get(QuerySample) must_== result
      ok
    }
    "Insert and Retrieve where Query EndTime and Cache EndTime is Same" in {
      val CacheStartTime = "2016-04-01T00:00:00.000Z"
      val CacheEndTime = "2016-12-01T00:00:00.000Z"
      val TimeFilter = FilterStatement(TestQuery.createAt, None, Relation.inRange, Seq(CacheStartTime, CacheEndTime))
      val Filter = Seq(TimeFilter)
      val CacheQuerySample = new Query(TestQuery.TwitterDataSet, Seq.empty, Seq.empty,Filter, Seq.empty, None)

      val result: JsValue = Json.obj(
        "name" -> "Watership Down",
        "location" -> Json.obj("lat" -> 51.235685, "long" -> -1.309197)
      )
      val cache : Cache = new Cache()
      cache.insert(CacheQuerySample, result) must_== true
      val QueryStartTime = "2016-01-01T00:00:00.000Z"
      val QueryEndTime = "2016-12-01T00:00:00.000Z"
      val QueryTimeFilter = FilterStatement(TestQuery.createAt, None, Relation.inRange, Seq(QueryStartTime, QueryEndTime))
      val QueryFilter = Seq(QueryTimeFilter)
      val QuerySample = new Query(TestQuery.TwitterDataSet, Seq.empty, Seq.empty,QueryFilter, Seq.empty, None)
      cache.get(QuerySample) must_== result
      ok
    }
  }
}
