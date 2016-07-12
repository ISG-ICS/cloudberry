package edu.uci.ics.cloudberry.zion.model.schema

trait IFunction {
  def name: String

  def args: Map[String, AnyVal]
}

trait TransformFunc extends IFunction {
  override val args: Map[String, AnyVal] = Map.empty
}

/**
  * unnest a bag. it should be ONLY used in the group by statement
  */
case object Unnest extends TransformFunc {
  override val name = "unnest"
}

case class Level(val level : String) extends TransformFunc {
  override val name = "level"
  override val args = Map("level" -> level)
}

trait Scale {
  def scale: Int
}

// Number
case class Bin(override val scale: Int) extends TransformFunc with Scale {
  override val name = "bin"
  override val args = Map("scale" -> scale)
}

// Time
sealed abstract class TimeStampScale(override val scale: Int) extends Scale with TransformFunc {
  def x: Int

  override val args = Map("x" -> x)
}

case class Second(override val x: Int = 1) extends TimeStampScale(1 * x) with TransformFunc {
  override val name = "second"
}

case class Minute(override val x: Int = 1) extends TimeStampScale(60 * x) with TransformFunc {
  override val name = "minute"
}

case class Hour(override val x: Int = 1) extends TimeStampScale(60 * 60 * x) with TransformFunc {
  override val name = "hour"
}

case class Day(override val x: Int = 1) extends TimeStampScale(60 * 60 * 24 * x) with TransformFunc {
  override val name = "day"
}

case class Week(override val x: Int = 1) extends TimeStampScale(60 * 60 * 24 * 7 * x) with TransformFunc {
  override val name = "week"
}

case class Month(override val x: Int = 1) extends TimeStampScale(60 * 60 * 24 * 7 * 30 * x) with TransformFunc {
  override val name = "month"
}

case class Year(override val x: Int = 1) extends TimeStampScale(60 * 60 * 24 * 7 * 365 * x) with TransformFunc {
  override val name = "year"
}

// Cell
sealed class GeoCellScale(override val scale: Int) extends Scale

case object GeoCell001X extends GeoCellScale(10) with TransformFunc {
  override def name: String = "geo-cell-001x"
}

case object GeoCell01X extends GeoCellScale(100) with TransformFunc {
  override def name: String = "geo-cell-01x"
}

case object GeoCell1X extends GeoCellScale(1000) with TransformFunc {
  override def name: String = "geo-cell-1x"
}

trait AggregateFunc extends IFunction {
  def args: Map[String, AnyVal] = Map.empty
}

case object Count extends AggregateFunc {
  override val name = "count"
}

case object Max extends AggregateFunc {
  override val name = "max"
}

case object Min extends AggregateFunc {
  override val name = "min"
}

case object Sum extends AggregateFunc {
  override val name = "sum"
}

case object DistinctCount extends AggregateFunc {
  override val name = "distinctCount"
}

case class TopK(val k: Int) extends AggregateFunc {
  override val name = "topK"
  override val args: Map[String, AnyVal] = Map("k" -> k)
}



