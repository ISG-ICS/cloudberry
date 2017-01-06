package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.impl.QueryPlanner.SortOrder
import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import play.api.libs.json._

class QueryPlannerTest extends Specification {

  import TestQuery._

  val filter = Seq(textFilter, timeFilter, stateFilter)
  val group = GroupStatement(Seq(byHour, byState), Seq(aggrCount))
  val queryCount = Query(TwitterDataSet, Seq.empty, filter, Seq.empty, Some(group), None)
  val groupTag = GroupStatement(Seq(byTag), Seq(aggrCount))
  val queryTag = new Query(TwitterDataSet, Seq.empty, filter, Seq(unnestHashTag), Some(groupTag), Some(selectTop10Tag))
  val querySample = new Query(TwitterDataSet, Seq.empty, filter, Seq.empty, None, Some(selectRecent))

  val planner = new QueryPlanner

  val zikaFullStats = Stats(sourceInterval.getStart, sourceInterval.getEnd, sourceInterval.getEnd, 50)
  val zikaFullYearViewInfo = DataSetInfo("zika", Some(zikaCreateQuery), schema, sourceInterval, zikaFullStats)

  "QueryPlanner" should {
    "makePlan ask source if the view is empty" in {
      planner.makePlan(queryCount, sourceInfo, Seq.empty)._1 must_== Seq(queryCount)
    }
    "should suggest a keyword view if hasn't found it " in {
      val queries = planner.suggestNewView(queryCount, sourceInfo, Seq.empty)
      queries.size must_== 2
      queries.exists(_.dataset == QueryPlanner.getViewKey(TwitterDataSet, "zika")) must_== true
      queries.exists(_.dataset == QueryPlanner.getViewKey(TwitterDataSet, "virus")) must_== true
      queries.exists(_.query == zikaCreateQuery) must_== true
      queries.exists(_.query == Query(TwitterDataSet, filter = Seq(virusFilter))) must_== true
    }
    "makePlan should choose a smaller view" in {

      val virusCreateQuery = Query(TwitterDataSet, filter = Seq(virusFilter))
      val virusStats = zikaFullStats.copy(cardinality = 500)
      val virusFullYearViewInfo = DataSetInfo("virus", Some(virusCreateQuery), schema, sourceInterval, virusStats)
      val (queries, _) = planner.makePlan(queryCount, sourceInfo, Seq(zikaFullYearViewInfo, virusFullYearViewInfo))
      queries.size must_== 1
      queries.head must_== queryCount.copy(dataset = zikaFullYearViewInfo.name)
    }
    "makePlan should only ask the view without touching source if it is sufficient to solve the query" in {
      val (queries, _) = planner.makePlan(queryCount, sourceInfo, Seq(zikaFullYearViewInfo))
      queries.size must_== 1
      queries.head must_== queryCount.copy(dataset = zikaFullYearViewInfo.name)
    }
    "makePlan should omit the redundant filter from query if it covers view.createQuery" in {

      val queryTimeFilter = FilterStatement("create_at", None, Relation.inRange, Seq("2016-01-01T00:00:00.000Z", "2016-12-01T00:00:00.000Z"))
      val queryZika = Query(
        dataset = TwitterDataSet,
        filter = Seq(
          FilterStatement("text", None, Relation.contains, Seq("zika")),
          queryTimeFilter
        ),
        groups = Some(group))
      val (queries, _) = planner.makePlan(queryZika, sourceInfo, Seq(zikaFullYearViewInfo))
      queries.head must_== Query(dataset = zikaFullYearViewInfo.name, filter = Seq(queryTimeFilter), groups = Some(group))
      queries.size must_== 1
    }
    "makePlan should ask the view and the source if view can not cover the query" in {
      val (queries, _) = planner.makePlan(queryCount, sourceInfo, Seq(zikaHalfYearViewInfo))
      queries.size must_== 2
      queries.exists(_.dataset == zikaHalfYearViewInfo.name) must_== true
      queries.exists(_.dataset == TwitterDataSet) must_== true
    }
    "makePlan generate a merger to merge the count query result" in {
      import QueryPlanner._
      val (_, mergerX) = planner.makePlan(queryCount, sourceInfo, Seq(zikaHalfYearViewInfo))
      val merger = mergerX.asInstanceOf[Merger]
      merger must_== Merger(Seq("hour", "state"), Map("count" -> Count), Map.empty, Set.empty, None)
    }
    "makePlan generate a merger to merge the hashtag query result" in {
      import QueryPlanner._
      val (_, mergerX) = planner.makePlan(queryTag, sourceInfo, Seq(zikaHalfYearViewInfo))
      val merger = mergerX.asInstanceOf[Merger]
      merger must_== Merger(Seq("tag"), Map("count" -> Count), Map("count" -> SortOrder.DSC), Set.empty, Some(10))
    }
    "makePlan generate a merger to merge the sample query result" in {
      import QueryPlanner._
      val (_, mergerX) = planner.makePlan(querySample, sourceInfo, Seq(zikaHalfYearViewInfo))
      val merger = mergerX.asInstanceOf[Merger]
      merger must_== Merger(Seq.empty, Map.empty, Map("create_at" -> SortOrder.DSC), Set("create_at", "id", "user.id"), Some(100))
    }
  }

  val timeCubeResult1 = JsArray(1 to 5 map { id =>
    Json.obj("day" -> JsString(s"d$id"), "state" -> JsNumber(id), "count" -> JsNumber(id * 100), "sum" -> JsNumber(id * id))
  })
  val timeCubeResult2 = JsArray(2 to 6 map { id =>
    Json.obj("day" -> JsString(s"d$id"), "state" -> JsNumber(id), "count" -> JsNumber(id * 100), "sum" -> JsNumber(id * id))
  })

  val aggrMap = Map("count" -> Count, "sum" -> Sum)

  "QueryPlanner$" should {
    "merge multiple groupby and sort JsArray result" in {
      val ret = QueryPlanner.merge(Seq(timeCubeResult1, timeCubeResult2), Seq("day", "state"), aggrMap, Map("day" -> SortOrder.ASC), Set.empty, None)
      ret must_== Json.parse(
        """
          |[ {
          |  "day" : "d1",
          |  "state" : 1,
          |  "count" : 100,
          |  "sum" : 1
          |}, {
          |  "day" : "d2",
          |  "state" : 2,
          |  "count" : 400,
          |  "sum" : 8
          |}, {
          |  "day" : "d3",
          |  "state" : 3,
          |  "count" : 600,
          |  "sum" : 18
          |}, {
          |  "day" : "d4",
          |  "state" : 4,
          |  "count" : 800,
          |  "sum" : 32
          |}, {
          |  "day" : "d5",
          |  "state" : 5,
          |  "count" : 1000,
          |  "sum" : 50
          |}, {
          |  "day" : "d6",
          |  "state" : 6,
          |  "count" : 600,
          |  "sum" : 36
          |} ]
        """.stripMargin)
    }
    "merge multiple sort only JsArray result" in {
      val timeCubeResult3 = JsArray(2 to 6 map { id =>
        Json.obj("day" -> JsString(s"d$id"), "state" -> JsNumber(id + 10), "count" -> JsNumber(id * 100), "sum" -> JsNumber(id * id))
      })

      val ret = QueryPlanner.merge(Seq(timeCubeResult1, timeCubeResult3), Seq.empty, Map.empty, Map("count" -> SortOrder.DSC), Set("day", "state", "count"), Some(8))
      ret must_== Json.parse(
        """
          |[ {
          |  "day" : "d6",
          |  "state" : 16,
          |  "count" : 600
          |}, {
          |  "day" : "d5",
          |  "state" : 5,
          |  "count" : 500
          |}, {
          |  "day" : "d5",
          |  "state" : 15,
          |  "count" : 500
          |}, {
          |  "day" : "d4",
          |  "state" : 4,
          |  "count" : 400
          |}, {
          |  "day" : "d4",
          |  "state" : 14,
          |  "count" : 400
          |}, {
          |  "day" : "d3",
          |  "state" : 3,
          |  "count" : 300
          |}, {
          |  "day" : "d3",
          |  "state" : 13,
          |  "count" : 300
          |}, {
          |  "day" : "d2",
          |  "state" : 2,
          |  "count" : 200
          |}]
        """.stripMargin)
    }
    "skip empty result from the input JsArrays" in {
      val ret = QueryPlanner.merge(Seq(timeCubeResult1, JsArray()), Seq("day", "state"), aggrMap, Map("count" -> SortOrder.DSC), Set.empty, None)
      ret must_== Json.parse(
        """
          |[ {
          |  "day" : "d5",
          |  "state" : 5,
          |  "count" : 500,
          |  "sum" : 25
          |}, {
          |  "day" : "d4",
          |  "state" : 4,
          |  "count" : 400,
          |  "sum" : 16
          |}, {
          |  "day" : "d3",
          |  "state" : 3,
          |  "count" : 300,
          |  "sum" : 9
          |}, {
          |  "day" : "d2",
          |  "state" : 2,
          |  "count" : 200,
          |  "sum" : 4
          |}, {
          |  "day" : "d1",
          |  "state" : 1,
          |  "count" : 100,
          |  "sum" : 1
          |} ]
        """.stripMargin)
    }
    "merge aggregate only JsArray result into on JsArray" in {
      ok
    }
    "merge empty seq of JsArray result into one JsArray.empty" in {
      val ret = QueryPlanner.merge(Seq(JsArray()), Seq("day", "state"), aggrMap, Map("count" -> SortOrder.DSC), Set.empty, None)
      ret must_== JsArray()
    }
  }
}
