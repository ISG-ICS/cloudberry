package edu.uci.ics.cloudberry.zion.model.schema

import edu.uci.ics.cloudberry.zion.model.datastore.FieldNotFound
import edu.uci.ics.cloudberry.zion.model.schema.DataType.DataType
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

trait Field {
  val name: String
  val dataType: DataType
  val isOptional: Boolean = false

  def as(name: String): Field
}

object Field {
  def apply(name: String, dataType: DataType, isOptional: Boolean = false): Field = {
    dataType match {
      case DataType.Number => NumberField(name, isOptional)
      case DataType.Time => TimeField(name, isOptional)
      case DataType.String => StringField(name, isOptional)
      case DataType.Text => TextField(name, isOptional)
      case DataType.Point => PointField(name, isOptional)
      case DataType.Boolean => PointField(name, isOptional)
      case _ => ???
    }
  }

}

case class NumberField(override val name: String, override val isOptional: Boolean = false) extends Field {
  override val dataType = DataType.Number

  def as(name: String) = NumberField(name, isOptional)

}

case class TimeField(override val name: String, override val isOptional: Boolean = false)
  extends Field {
  override val dataType = DataType.Time

  def as(name: String) = TimeField(name, isOptional)

}

object TimeField {
  val TimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
}

case class StringField(override val name: String, override val isOptional: Boolean = false)
  extends Field {
  override val dataType = DataType.String

  def as(name: String) = StringField(name, isOptional)
}

case class TextField(override val name: String, override val isOptional: Boolean = false)
  extends Field {
  override val dataType = DataType.Text

  def as(name: String) = TextField(name, isOptional)

}

case class PointField(override val name: String, override val isOptional: Boolean = false)
  extends Field {
  override val dataType = DataType.Point

  def as(name: String) = PointField(name, isOptional)

}

case class BooleanField(override val name: String, override val isOptional: Boolean = false)
  extends Field {
  override val dataType = DataType.Boolean

  def as(name: String) = BooleanField(name, isOptional)


}

trait NestedField extends Field {
  val innerType: DataType
}

case class BagField(override val name: String,
                    override val innerType: DataType,
                    override val isOptional: Boolean = false
                   ) extends NestedField {
  override val dataType = DataType.Bag

  def as(name: String) = BagField(name, innerType, isOptional)

}

/**
  * Hierarchy field type
  *
  * @param name
  * @param innerType
  * @param levels the level to actual field name mapping
  */
case class HierarchyField(override val name: String,
                          override val innerType: DataType,
                          val levels: Seq[(String, String)],
                          override val isOptional: Boolean = false
                         ) extends NestedField {
  override val dataType = DataType.Hierarchy

  def as(name: String) = HierarchyField(name, innerType, levels, isOptional)
}

case class RecordField(override val name: String, val schema: Schema, override val isOptional: Boolean = false) extends Field {
  override val dataType = DataType.Record

  def as(name: String) = RecordField(name, schema, isOptional)

}

case object AllField extends Field {
  override val name: String = "*"
  override val dataType: DataType = DataType.Record
  override val isOptional: Boolean = false

  def as(name: String) = ???

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
case class Schema(typeName: String,
                  dimension: Seq[Field],
                  measurement: Seq[Field],
                  primaryKey: Seq[Field],
                  timeField: Option[TimeField]
                 ) {

  private val dimensionMap: Map[String, Field] = dimension.map(f => f.name -> f).toMap
  private val measurementMap: Map[String, Field] = measurement.map(f => f.name -> f).toMap

  def apply(field: String): Field =
    fieldMap.get(field) match {
      case Some(f) => f
      case None => throw new FieldNotFound(field)
    }

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
