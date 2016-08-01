package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._
import org.joda.time.Interval

class QueryOptimizer {

  def makePlan(query: Query, source: DataSetInfo, views: Seq[DataSetInfo]): Seq[Query] = {

    val applicableViews = filterViews(query.filter, views)
    val matchedViews = filterView(query.groups, applicableViews)
    //TODO currently only get the best one
    val bestView = selectBestView(matchedViews)
    splitQuery(query, source, bestView)
  }

  def suggestNewView(query: Query, views: Seq[DataSetInfo]): Seq[CreateView] = ???

  private def filterViews(filter: Seq[FilterStatement], views: Seq[DataSetInfo]): Seq[DataSetInfo] = {
    views.filter { view =>
      view.createQueryOpt.exists { viewQuery: Query =>
        //TODO should remove the time dimension.
        filter.forall(queryFilter => viewQuery.filter.exists(viewFilter => viewFilter.include(queryFilter)))
      }
    }
  }

  private def filterView(groups: Option[GroupStatement], views: Seq[DataSetInfo]): Seq[DataSetInfo] = {
    groups match {
      case None => views.filter(_.createQueryOpt.exists(_.groups.isEmpty))
      case Some(group) =>
        views.filter { view =>
          view.createQueryOpt.exists { viewQuery: Query =>
            viewQuery.groups.forall(_.finerThan(group))
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
        val queryInterval = query.getTimeInterval
        val viewInterval = view.dataInterval
        val unCovered = getUnCoveredInterval(viewInterval, queryInterval)

        val seqBuilder = Seq.newBuilder[Query]

        seqBuilder += query.copy(dataset = view.name)
        for (interval <- unCovered) {
          seqBuilder += query.replaceInterval(interval)
        }
        seqBuilder.result()
    }
  }

  private def getUnCoveredInterval(viewInterval: Interval, queryInterval: Interval): Seq[Interval] = ???
}
