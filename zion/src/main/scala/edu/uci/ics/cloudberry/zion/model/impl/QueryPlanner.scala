package edu.uci.ics.cloudberry.zion.model.impl

import java.security.MessageDigest

import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.Interval

class QueryPlanner {

  import QueryPlanner._

  def makePlan(query: Query, source: DataSetInfo, views: Seq[DataSetInfo]): Seq[Query] = {

    val matchedViews = views.filter(view => view.createQueryOpt.exists(vq => vq.canSolve(query, source.schema)))
    //TODO currently only get the best one
    val bestView = selectBestView(matchedViews)
    splitQuery(query, source, bestView)
  }

  def suggestNewView(query: Query, source: DataSetInfo, views: Seq[DataSetInfo]): Seq[CreateView] = {
    //TODO currently only suggest the keyword subset views
    if (views.exists(v => v.createQueryOpt.exists(vq => vq.canSolve(query, source.schema)))) {
      Seq.empty[CreateView]
    } else {
      val keywordFilters = query.filter.filter(f => source.schema.fieldMap(f.fieldName).dataType == DataType.Text)
      keywordFilters.flatMap { kwFilter =>
        kwFilter.values.map { wordAny =>
          val word = wordAny.asInstanceOf[String]
          val wordFilter = FilterStatement(kwFilter.fieldName, None, Relation.contains, Seq(word))
          val wordQuery = Query(query.dataset, Seq.empty, Seq(wordFilter), Seq.empty, None, None)
          CreateView(getViewKey(query.dataset, word), wordQuery)
        }
      }
    }
  }

  private def selectBestView(matchedViews: Seq[DataSetInfo]): Option[DataSetInfo] = {
    if (matchedViews.isEmpty) {
      None
    } else {
      Some(matchedViews.min(Ordering.by((info: DataSetInfo) => info.stats.cardinality)))
    }
  }


  private def splitQuery(query: Query, source: DataSetInfo, bestView: Option[DataSetInfo]): Seq[Query] = {
    bestView match {
      case None => Seq(query)
      case Some(view) =>
        val queryInterval = query.getTimeInterval(source.schema.timeField).getOrElse(source.dataInterval)
        val viewInterval = view.dataInterval
        val unCovered = getUnCoveredInterval(viewInterval, queryInterval)

        val seqBuilder = Seq.newBuilder[Query]

        //TODO here is a very simple assumption that the schema is the same, what if the schema are different?
        seqBuilder += query.copy(dataset = view.name)
        for (interval <- unCovered) {
          seqBuilder += query.setInterval(source.schema.timeField, interval)
        }
        seqBuilder.result()
    }
  }

}

object QueryPlanner {

  def getUnCoveredInterval(dataInterval: Interval, queryInterval: Interval): Seq[Interval] = {
    val intersect = dataInterval.overlap(queryInterval)
    if (intersect == null) {
      return Seq(queryInterval)
    }
    val intervals = scala.collection.mutable.ArrayBuffer.empty[Interval]
    if (queryInterval.getStartMillis < intersect.getStartMillis) {
      intervals += new Interval(queryInterval.getStartMillis, intersect.getStartMillis)
    }
    if (intersect.getEndMillis < queryInterval.getEndMillis) {
      intervals += new Interval(intersect.getEndMillis, queryInterval.getEndMillis)
    }
    intervals
  }

  def getViewKey(sourceName: String, keyword: String): String = {
    sourceName + "_" + MessageDigest.getInstance("MD5").digest(keyword.getBytes("UTF-8")).map("%02x" format _).mkString
  }
}
