package edu.uci.ics.cloudberry.zion.model

object SpatialLevels extends Enumeration {
  val State, County, City, Point = Value
}

object TimeLevels extends Enumeration {
  val Year, Quarter, Month, Day, Hour, Minute, Second, TimeStamp = Value
}

case class SummaryLevel(spatialLevel: SpatialLevels.Value, timeLevel: TimeLevels.Value) {
  def isFinerThan(request: SummaryLevel): Boolean = {
    request.spatialLevel <= this.spatialLevel && request.timeLevel <= this.timeLevel
  }
}

object AggFunctionTypes extends Enumeration {
  val Count, Sum, TopK = Value
}

