package edu.uci.ics.cloudberry.zion.model.schema

//TODO support nested type
object DataType extends Enumeration {
  type DataType = Value
  val Number, Time, String, Text, Bag, Point, Hierarchy = Value
}

object Relation extends Enumeration {
  type Relation = Value
  val == = Value("=")
  val != = Value("!=")
  val <= = Value("<=")
  val >= = Value(">=")
  val > = Value(">")
  val < = Value("<")
  val in = Value("in")
  val inRange = Value("inRange")
  val contains = Value("contains")
  val isTrue = Value("true")
}

//TODO implement these logic later
object StringRelation extends Enumeration {
  type StringRelation = Value
  val contains = Value("contains")
  val startsWith = Value("starts-with")
  val endsWith = Value("ends-with")
  val matches = Value("matches")
  val ~= = Value("~=")
}

trait Field {
  def name: String
  def dataType: DataType.DataType
}

trait NestedField extends Field {
  def innerType : DataType.DataType
}

trait HierarchyField extends NestedField {
  def levels : Array[String]
}

/**
  * Including "interesting" fields which could be used as group keys.
  */
trait Dimension {
  def fields : Seq[Field]
}

/**
  * Quantitative fields which usually used to get the min/max/avg/std values.
  */
trait Measurement {
  def fields : Seq[Field]
}


//TODO when UI get the schema, it should know which is dimension/measure, functions that can apply onto it, etc.
// so that it won't ask for a inapplicable function such that get the max to a string field
final class Schema(val dataset: String, val dimension: Seq[Field], val measurement: Seq[Field]) {
}
