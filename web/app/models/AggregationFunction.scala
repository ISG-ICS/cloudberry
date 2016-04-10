package models

sealed trait AggregationFunction {
  def name: String
}

case object Min extends AggregationFunction {
  val name = "MIN"
}

case object Max extends AggregationFunction {
  val name = "Max"
}

case object Sum extends AggregationFunction {
  val name = "SUM"
}

case object Count extends AggregationFunction {
  val name = "COUNT"
}

case object AVG extends AggregationFunction {
  val name = "AVG"
}

