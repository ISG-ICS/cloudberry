package edu.uci.ics.cloudberry.zion.model.schema

import edu.uci.ics.cloudberry.zion.model.schema.DataType.DataType

//TODO support nested type
object DataType extends Enumeration {
  type DataType = Value
  val Number, Time, Point, Boolean, String, Text, Bag, Hierarchy = Value
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
  val isTrue = Value("true")
  val isFalse = Value("false")
  // string relations
  val contains = Value("contains")
  // the operation for string and text type is different
  val startsWith = Value("startsWith")
  val endsWith = Value("endsWith")
  val matches = Value("matches")
  val ~= = Value("~=")
}

class Field(val name: String, val dataType: DataType) {
}

case class NumberField(override val name: String) extends Field(name, DataType.Number)

case class TimeField(override val name: String) extends Field(name, DataType.Time)

case class StringField(override val name: String) extends Field(name, DataType.String)

case class TextField(override val name: String) extends Field(name, DataType.Text)

case class PointField(override val name: String) extends Field(name, DataType.Point)

case class BooleanField(override val name: String) extends Field(name, DataType.Boolean)

class NestedField(override val name: String,
                  override val dataType: DataType,
                  val innerType: DataType
                 ) extends Field(name, dataType) {
}

case class BagField(override val name: String,
                    override val innerType: DataType
                   ) extends NestedField(name, DataType.Bag, innerType)

case class HierarchyField(override val name: String,
                          override val innerType: DataType,
                          val levels: Map[String, String]
                         ) extends NestedField(name, DataType.Hierarchy, innerType) {
}

/**
  * Including "interesting" fields which could be used as group keys.
  */
trait Dimension {
  def fields: Seq[Field]
}

/**
  * Quantitative fields which usually used to get the min/max/avg/std values.
  */
trait Measurement {
  def fields: Seq[Field]
}


//TODO when UI get the schema, it should know which is dimension/measure, functions that can apply onto it, etc.
// so that it won't ask for a inapplicable function such that get the max to a string field
final class Schema(val dataset: String, val dimension: Seq[Field], val measurement: Seq[Field]) {
  def getFieldNames: Seq[String] = dimension.map(_.name) ++ measurement.map(_.name)

  private val dimensionMap: Map[String, Field] = dimension.map(f => f.name -> f).toMap
  private val measurementMap: Map[String, Field] = measurement.map(f => f.name -> f).toMap

}

object Schema {

  import DataType._
  import Relation.Relation

  val BasicRelSet: Set[Relation] = Set(Relation.==, Relation.!=, Relation.<=, Relation.>=, Relation.>, Relation.<, Relation.in)
  val StringRelSet: Set[Relation] = Set(Relation.==, Relation.!=, Relation.in, Relation.contains, Relation.startsWith, Relation.endsWith, Relation.matches, Relation.~=)

  val Type2Relations: Map[DataType, Set[Relation]] = Map(
    Number -> (BasicRelSet + Relation.inRange),
    Time -> (BasicRelSet + Relation.inRange),
    Boolean -> Set(Relation.isTrue, Relation.isFalse),
    Point -> Set(Relation.in, Relation.inRange),
    String -> StringRelSet,
    Text -> Set(Relation.contains),
    Bag -> Set(Relation.contains),
    Hierarchy -> Set()
  )
}
