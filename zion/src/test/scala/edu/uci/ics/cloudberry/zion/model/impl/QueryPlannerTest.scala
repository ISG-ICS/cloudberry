package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema.{FilterStatement, GroupStatement, Query, Relation}
import org.joda.time.{DateTime, Interval}
import org.specs2.mutable.Specification

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

  "QueryPlannerTest" should {
    "makePlan ask source if the view is empty" in {
      planner.makePlan(queryCount, sourceInfo, Seq.empty) must_== Seq(queryCount)
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
      val queries = planner.makePlan(queryCount, sourceInfo, Seq(zikaFullYearViewInfo, virusFullYearViewInfo))
      queries.size must_== 1
      queries.head must_== queryCount.copy(dataset = zikaFullYearViewInfo.name)
    }
    "makePlan should only ask the view without touching source if it is enough to solve the query" in {
      val queries = planner.makePlan(queryCount, sourceInfo, Seq(zikaFullYearViewInfo))
      queries.size must_== 1
      queries.head must_== queryCount.copy(dataset = zikaFullYearViewInfo.name)
    }
    "makePlan should ask the view and the source if view can not cover the query" in {
      val queries = planner.makePlan(queryCount, sourceInfo, Seq(zikaHalfYearViewInfo))
      queries.size must_== 2
      queries.exists(_.dataset == zikaHalfYearViewInfo.name) must_== true
      queries.exists(_.dataset == TwitterDataSet) must_== true
      ok
    }
  }
}
