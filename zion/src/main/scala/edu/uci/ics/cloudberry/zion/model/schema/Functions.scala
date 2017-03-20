package edu.uci.ics.cloudberry.zion.model.schema

import edu.uci.ics.cloudberry.zion.model.datastore.QueryParsingException
import edu.uci.ics.cloudberry.zion.model.schema.DataType.DataType

sealed trait IFunction {
  def name: String

  /**
    * A set of acceptable types for this function
    * @return
    */
  def acceptType: Set[DataType]

  /**
    * Return the result type when apply this function to the given field
    * @param field
    * @return
    */
  def resultType(field: Field): DataType

  /**
    * Return the result field when apply this function to the given field
    * @param field
    * @return
    */
  def apply(field: Field): Field = {
    IFunction.validateField(this, field)
    Field(s"$name(${field.name})", resultType(field))
  }

}

object IFunction {
  def validateField(function: IFunction, field: Field): Unit = {
    if (!function.acceptType.contains(field.dataType)) {
      throw new QueryParsingException(s"${function.name} requires type ${function.acceptType.mkString(" or ")}, but ${field.dataType} is provided.")
    }
  }
}

sealed trait TransformFunc extends IFunction

object TransformFunc {
  val ToString = "toString"
}

sealed trait GroupFunc extends IFunction

object GroupFunc {
  val Bin = "bin"
  val Interval = "interval"
  val Level = "level"
  val GeoCellTenth = "geoCellTenth"
  val GeoCellHundredth = "geoCellHundredth"
  val GeoCellThousandth = "geoCellThousandth"

  val All = Set(Bin, Level, GeoCellTenth, GeoCellHundredth, GeoCellThousandth)
}

trait AggregateFunc extends IFunction {
  def args: Map[String, AnyVal] = Map.empty
}

object AggregateFunc {
  val Count = "count"
  val Min = "min"
  val Max = "max"
  val Sum = "sum"
  val Avg = "avg"
  val DistinctCount = "distinctCount"
  val TopK = "topK"

  val All = Set(Count, Min, Max, Sum, Avg, DistinctCount, TopK)
}

case class Level(levelTag: String) extends GroupFunc {
  override val name = GroupFunc.Level

  override val acceptType: Set[DataType] = Set(DataType.Hierarchy)

  override def resultType(field: Field): DataType = {
    val hierarchyField = field.asInstanceOf[HierarchyField]
    hierarchyField.innerType
  }
}

trait Scale {
  def scale: Int
}

/**
  * Produce the bin number of the group for those number type field
  *
  * @param scale the int number to compare the scales
  */
case class Bin(override val scale: Int) extends GroupFunc with Scale {
  override val name = GroupFunc.Bin

  override def acceptType: Set[DataType] = Set(DataType.Number)

  override def resultType(field: Field): DataType = DataType.Number
}

case class Interval(unit: TimeUnit.Value, x: Int = 1) extends GroupFunc with Scale {
  override def name: String = GroupFunc.Interval

  override def acceptType: Set[DataType] = Set(DataType.Time)

  override def resultType(field: Field): DataType = DataType.Time

  override val scale: Int = {
    import TimeUnit._
    unit match {
      case Second => x
      case Minute => x * 60
      case Hour => x * 60 * 60
      case Day => x * 60 * 60 * 24
      case Week => x * 60 * 60 * 24 * 7
      case Month => x * 60 * 60 * 24 * 30
      case Year => x * 60 * 60 * 24 * 365
    }
  }
}

object TimeUnit extends Enumeration {
  type TimeUnit = Value
  val Second = Value("second")
  val Minute = Value("minute")
  val Hour = Value("hour")
  val Day = Value("day")
  val Week = Value("week")
  val Month = Value("month")
  val Year = Value("year")
}

// Cell
sealed class GeoCellScale(override val scale: Int) extends Scale

case object GeoCellThousandth extends GeoCellScale(10000) with GroupFunc {
  override def name: String = GroupFunc.GeoCellThousandth

  override def acceptType: Set[DataType] = Set(DataType.Point)

  override def resultType(field: Field): DataType = DataType.Point

}

case object GeoCellHundredth extends GeoCellScale(100000) with GroupFunc {
  override def name: String = GroupFunc.GeoCellHundredth

  override def acceptType: Set[DataType] = Set(DataType.Point)

  override def resultType(field: Field): DataType = DataType.Point

}

case object GeoCellTenth extends GeoCellScale(1000000) with GroupFunc {
  override def name: String = GroupFunc.GeoCellTenth

  override def acceptType: Set[DataType] = Set(DataType.Point)

  override def resultType(field: Field): DataType = DataType.Point
}

case object Count extends AggregateFunc {
  override val name = AggregateFunc.Count

  override def acceptType: Set[DataType] = DataType.values

  override def resultType(field: Field): DataType = DataType.Number
}

case object Max extends AggregateFunc {
  override val name = AggregateFunc.Max

  override def acceptType: Set[DataType] = Set(DataType.Number, DataType.Time)

  override def resultType(field: Field): DataType = field.dataType

}

case object Min extends AggregateFunc {
  override val name = AggregateFunc.Min

  override def acceptType: Set[DataType] = Set(DataType.Number, DataType.Time)

  override def resultType(field: Field): DataType = field.dataType
}

case object Sum extends AggregateFunc {
  override val name = AggregateFunc.Sum

  override def acceptType: Set[DataType] = Set(DataType.Number)

  override def resultType(field: Field): DataType = DataType.Number
}

case object Avg extends AggregateFunc {
  override val name = AggregateFunc.Avg

  override def acceptType: Set[DataType] = Set(DataType.Number)

  override def resultType(field: Field): DataType = DataType.Number
}

case object DistinctCount extends AggregateFunc {
  override val name = AggregateFunc.DistinctCount

  override def acceptType: Set[DataType] = DataType.values

  override def resultType(field: Field): DataType = DataType.Number
}

case class TopK(val k: Int) extends AggregateFunc {
  override val name = AggregateFunc.TopK

  override def acceptType: Set[DataType] = DataType.values

  override def resultType(field: Field): DataType = field.dataType
}

case object ToString extends TransformFunc {
  override def name: String = TransformFunc.ToString

  override def acceptType: Set[DataType] = Set(DataType.Number)

  override def resultType(field: Field): DataType = DataType.String
}
