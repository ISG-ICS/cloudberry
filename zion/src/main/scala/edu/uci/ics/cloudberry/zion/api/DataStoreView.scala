package edu.uci.ics.cloudberry.zion.api

import org.joda.time.DateTime

import scala.concurrent.duration.Duration

trait DataStoreView extends DataStore {
  def source: DataStore

  def queryTemplate: DBQuery

  def updateCycle: Duration

  def startTime: DateTime

  def lastVisitTime: DateTime

  def lastUpdateTime: DateTime

  def visitTimes: Int
}

trait SubSetView extends DataStoreView

trait SummaryView extends DataStoreView {
  def summaryLevel: SummaryLevel
}


