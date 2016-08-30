package edu.uci.ics.cloudberry.zion.model.schema

import edu.uci.ics.cloudberry.zion.model.schema.DataType.DataType
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

//TODO support nested type
object DataType extends Enumeration {
  type DataType = Value
  val Number = Value("Number")
  val Time = Value("Time")
  val Point = Value("Point")
  val Boolean = Value("Boolean")
  val String = Value("String")
  val Text = Value("Text")
  val Bag = Value("Bag")
  val Hierarchy = Value("Hierarchy")
  val Record = Value("Record")
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

class Field(val name: String, val dataType: DataType, val isOptional: Boolean = false) {
}

case class NumberField(override val name: String, override val isOptional: Boolean = false)
  extends Field(name, DataType.Number, isOptional)

case class TimeField(override val name: String, override val isOptional: Boolean = false)
  extends Field(name, DataType.Time, isOptional)

object TimeField {
  val TimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
}

case class StringField(override val name: String, override val isOptional: Boolean = false)
  extends Field(name, DataType.String, isOptional)

case class TextField(override val name: String, override val isOptional: Boolean = false)
  extends Field(name, DataType.Text, isOptional)

case class PointField(override val name: String, override val isOptional: Boolean = false)
  extends Field(name, DataType.Point, isOptional)

case class BooleanField(override val name: String, override val isOptional: Boolean = false)
  extends Field(name, DataType.Boolean, isOptional)

class NestedField(override val name: String,
                  override val dataType: DataType,
                  val innerType: DataType,
                  override val isOptional: Boolean
                 ) extends Field(name, dataType, isOptional) {
}

case class BagField(override val name: String,
                    override val innerType: DataType,
                    override val isOptional: Boolean
                   ) extends NestedField(name, DataType.Bag, innerType, isOptional)

/**
  * Hierarchy field type
  *
  * @param name
  * @param innerType
  * @param levels the level to actual field name mapping
  */
case class HierarchyField(override val name: String,
                          override val innerType: DataType,
                          levels: Seq[(String, String)]
                         ) extends NestedField(name, DataType.Hierarchy, innerType, false)

case class RecordField(override val name: String, schema: Schema, override val isOptional: Boolean)
  extends Field(name, DataType.Record, isOptional)

case object AllField extends Field("*", DataType.Record, false)

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
case class Schema(typeName: String,
                  dimension: Seq[Field],
                  measurement: Seq[Field],
                  primaryKey: Seq[String],
                  timeField: String
                 ) {

  private val dimensionMap: Map[String, Field] = dimension.map(f => f.name -> f).toMap
  private val measurementMap: Map[String, Field] = measurement.map(f => f.name -> f).toMap

  val fieldMap: Map[String, Field] = dimensionMap ++ measurementMap ++ Map(AllField.name -> AllField)
}

object Schema {

  import DataType._
  import Relation.Relation

  val BasicRelSet: Set[Relation] = Set(Relation.==, Relation.!=, Relation.<=, Relation.>=, Relation.>, Relation.<)
  val StringRelSet: Set[Relation] = Set(Relation.==, Relation.!=, Relation.in, Relation.contains, Relation.startsWith, Relation.endsWith, Relation.matches, Relation.~=)

  val Type2Relations: Map[DataType, Set[Relation]] = Map(
    Number -> (BasicRelSet + Relation.in + Relation.inRange),
    Time -> (BasicRelSet + Relation.inRange),
    Boolean -> Set(Relation.isTrue, Relation.isFalse),
    Point -> Set(Relation.inRange),
    String -> StringRelSet,
    Text -> Set(Relation.contains),
    Bag -> Set(Relation.contains),
    Hierarchy -> Set()
  )
}
