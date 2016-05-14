package edu.uci.ics.cloudberry.zion.model

import edu.uci.ics.cloudberry.util.Rectangle
import org.joda.time.Interval

object Type extends Enumeration {
  val Number, String, Text, GeoPoint, Time, ID, Nested = Value
}

object ArithmeticRelation extends Enumeration {
  type ArithmeticRelation = Value
  val == = Value("=")
  val != = Value("!=")
  val <= = Value("<=")
  val >= = Value(">=")
  val > = Value(">")
  val < = Value("<")
}

object StringRelation extends Enumeration {
  type StringRelation = Value
  val Contains = Value("contains")
  val StartsWith = Value("starts-with")
  val EndsWith = Value("ends-with")
  val Matches = Value("matches")
  val == = Value("=")
}

object TextRelation extends Enumeration {
  val AND = Value
  //TODO val OR =
}

object GeoRelation extends Enumeration {
  val Intersect = Value
}

object TimeRelation extends Enumeration {
  val Within = Value
}

object IDRelation extends Enumeration {
  val In = Value
}

trait NPredicate {
  def fieldName: String
}

object IDValueType extends Enumeration {
  val String, NotString = Value
}

case class NumberPredicate(fieldName: String, relation: ArithmeticRelation.Value, value: AnyVal) extends NPredicate

case class StringPredicate(fieldName: String, relation: StringRelation.Value, value: String) extends NPredicate

case class TextPredicate(fieldName: String, relation: TextRelation.Value, value: String) extends NPredicate

case class GeoPredicate(fieldName: String, relation: GeoRelation.Value, rectangle: Rectangle) extends NPredicate

case class ETimePredicate(fieldName: String, relation: TimeRelation.Value, interval: Interval) extends NPredicate

case class IDPredicate(fieldName: String, relation: TimeRelation.Value, idSet: Set[Any]) extends NPredicate


trait Dimension {
  def fieldName: String

  def scale: Scale
}

trait Scale {
  def scale: Int
}

// Number
case class NumScale(override val scale: Int) extends Scale

// Time
sealed class TimeScale(override val scale: Int) extends Scale

case object TimeStamp extends TimeScale(1)

case object Minute extends TimeScale(2)

case object Hour extends TimeScale(3)

case object Day extends TimeScale(4)

case object Week extends TimeScale(5)

case object Month extends TimeScale(6)

case object Quarter extends TimeScale(7)

case object Year extends TimeScale(8)

// Geo
// Choropleth
sealed class ChoroplethScale(override val scale: Int) extends Scale

case object Neighborhood extends ChoroplethScale(1)

case object City extends ChoroplethScale(2)

case object County extends ChoroplethScale(3)

case object State extends ChoroplethScale(4)

// Cell
sealed class GeoCellScale(override val scale: Int) extends Scale

case object GeoLogLat extends GeoCellScale(1)

case object Geo001x extends GeoCellScale(2)

case object Geo01x extends GeoCellScale(3)

case object Geo1x extends GeoCellScale(4)

// Measure
trait Measurement

case object Count extends Measurement

case class Max(fieldName: String) extends Measurement

case class Min(fieldName: String) extends Measurement

case class Sum(fieldName: String) extends Measurement

case class DistinctCount(fieldName: String) extends Measurement

case class TopK(fieldName: String, k: Int) extends Measurement

case class Field(name: String, typeValue: Type.Value)

//TODO when UI get the schema, it should know which is dimension/measure, functions that can apply onto it, etc.
// so that it won't ask for a inapplicable function such that get the max to a string field
class Schema(fields: Map[String, Field]) {
  def field(name: String): Option[Field] = fields.get(name)
}

case class Query(dataSet: String, filters: Seq[NPredicate], dimensions: Seq[Dimension], measurements: Seq[Measurement])

case class DimensionValue(dimension: Dimension, value: Any)

case class MeasurementValue(measurement: Measurement, value: Any)

case class Result(dimensions: Seq[DimensionValue], measurements: Seq[MeasurementValue])
