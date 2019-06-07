package edu.uci.ics.cloudberry.zion.model.schema

import edu.uci.ics.cloudberry.zion.model.impl.UnresolvedSchema
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
  val isNull = Value("isNull")
  val isNotNull = Value("isNotNull")
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

sealed trait Field {
  val name: String
  val dataType: DataType
  val isOptional: Boolean = false

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

  /**
    * Copy a given field with a new name ('as' keyword in the query)
    * @param field
    * @param name
    * @return
    */
  def as(field: Field, name: String): Field = {
    field.dataType match {
      case DataType.Number => NumberField(name, field.isOptional)
      case DataType.Time => TimeField(name, field.isOptional)
      case DataType.String => StringField(name, field.isOptional)
      case DataType.Text => TextField(name, field.isOptional)
      case DataType.Point => PointField(name, field.isOptional)
      case DataType.Boolean => BooleanField(name, field.isOptional)
      case DataType.Hierarchy =>
        val hierarchyField = field.asInstanceOf[HierarchyField]
        HierarchyField(name, hierarchyField.innerType, hierarchyField.levels, hierarchyField.isOptional)
      case DataType.Record =>
        val recordField = field.asInstanceOf[RecordField]
        RecordField(name, recordField.schema, recordField.isOptional)
      case DataType.Bag =>
        val bagField = field.asInstanceOf[BagField]
        BagField(name, bagField.innerType, bagField.isOptional)
    }

  }

  /**
    * Copy a given field "innerType" with a new name, used in resolveUnnests
    * @param field
    * @param name
    * @return
    */
  def asInnerType(field: Field, name: String): Field = {
    val bagField = field.asInstanceOf[BagField]
    bagField.innerType match {
      case DataType.Number => NumberField(name, field.isOptional)
      case DataType.Time => TimeField(name, field.isOptional)
      case DataType.String => StringField(name, field.isOptional)
      case DataType.Text => TextField(name, field.isOptional)
      case DataType.Point => PointField(name, field.isOptional)
      case DataType.Boolean => BooleanField(name, field.isOptional)
    }
  }

}

case class NumberField(override val name: String, override val isOptional: Boolean = false) extends Field {
  override val dataType = DataType.Number

}

case class TimeField(override val name: String, override val isOptional: Boolean = false)
  extends Field {
  override val dataType = DataType.Time

}

object TimeField {
  val TimeFormat = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  val TimeFormatForSQL = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS")
}

case class StringField(override val name: String, override val isOptional: Boolean = false)
  extends Field {
  override val dataType = DataType.String

}

case class TextField(override val name: String, override val isOptional: Boolean = false)
  extends Field {
  override val dataType = DataType.Text

}

case class PointField(override val name: String, override val isOptional: Boolean = false)
  extends Field {
  override val dataType = DataType.Point

}

case class BooleanField(override val name: String, override val isOptional: Boolean = false)
  extends Field {
  override val dataType = DataType.Boolean

}

trait NestedField extends Field {
  val innerType: DataType
}

case class BagField(override val name: String,
                    override val innerType: DataType,
                    override val isOptional: Boolean = false
                   ) extends NestedField {
  override val dataType = DataType.Bag

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

}

case class RecordField(override val name: String, val schema: AbstractSchema, override val isOptional: Boolean = false) extends Field {
  override val dataType = DataType.Record

}

case object AllField extends Field {
  override val name: String = "*"
  override val dataType: DataType = DataType.Record
  override val isOptional: Boolean = false

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
abstract class AbstractSchema(typeName: String,
                              dimension: Seq[Field],
                              measurement: Seq[Field],
                              primaryKey: Seq[Field]
                             ) {

  private val dimensionMap: Map[String, Field] = dimension.map(f => f.name -> f).toMap
  private val measurementMap: Map[String, Field] = measurement.map(f => f.name -> f).toMap

  val fieldMap: Map[String, Field] = dimensionMap ++ measurementMap ++ Map(AllField.name -> AllField)

  def getTypeName: String = typeName
  def copySchema: AbstractSchema
  def toUnresolved: UnresolvedSchema
  def getPrimaryKey: Seq[Field] = primaryKey
}

case class Schema(typeName: String,
                  dimension: Seq[Field],
                  measurement: Seq[Field],
                  primaryKey: Seq[Field],
                  timeField: TimeField
                 ) extends AbstractSchema(typeName, dimension, measurement, primaryKey){

  override def copySchema: Schema = this.copy()

  override def toUnresolved: UnresolvedSchema = UnresolvedSchema(typeName, dimension, measurement, primaryKey.map(_.name), Some(timeField.name))
}

case class LookupSchema(typeName: String,
                        dimension: Seq[Field],
                        measurement: Seq[Field],
                        primaryKey: Seq[Field]
                       ) extends AbstractSchema(typeName, dimension, measurement, primaryKey) {

  override def copySchema: LookupSchema = this.copy()

  override def toUnresolved: UnresolvedSchema = UnresolvedSchema(typeName, dimension, measurement, primaryKey.map(_.name), None)
}

object AbstractSchema {

  import DataType._
  import Relation.Relation

  val BasicRelSet: Set[Relation] = Set(Relation.==, Relation.!=, Relation.<=, Relation.>=, Relation.>, Relation.<, Relation.isNull, Relation.isNotNull)
  val StringRelSet: Set[Relation] = Set(Relation.==, Relation.!=, Relation.in, Relation.contains, Relation.startsWith, Relation.endsWith, Relation.matches, Relation.~=, Relation.isNull, Relation.isNotNull)

  val Type2Relations: Map[DataType, Set[Relation]] = Map(
    Number -> (BasicRelSet + Relation.in + Relation.inRange),
    Time -> (BasicRelSet + Relation.inRange),
    Boolean -> Set(Relation.isTrue, Relation.isFalse, Relation.isNull, Relation.isNotNull),
    Point -> Set(Relation.inRange, Relation.isNull, Relation.isNotNull),
    String -> StringRelSet,
    Text -> Set(Relation.contains, Relation.isNull, Relation.isNotNull),
    Bag -> Set(Relation.contains),
    Hierarchy -> Set()
  )

}
