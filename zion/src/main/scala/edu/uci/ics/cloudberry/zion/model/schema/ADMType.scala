package edu.uci.ics.cloudberry.zion.model.schema

object ADMType extends Enumeration {
  type Type = Value
  val Boolean = Value("boolean")
  val Int64 = Value("int64")
  val Double = Value("double")
  val String = Value("string")
  val Point = Value("point")
  val DateTime = Value("datetime")

  val mapToSchema: Map[Type, DataType.DataType] = Map(Boolean -> DataType.Boolean,
                                                      Int64 -> DataType.Number,
                                                      Double -> DataType.Number,
                                                      String -> DataType.String,
                                                      Point -> DataType.Point,
                                                      DateTime -> DataType.Time)
}
