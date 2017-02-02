package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema.{NumberField, Schema, TimeField}

/**
  * Created by nishadgurav on 2/1/17.
  */
object LiteracyDataStore {
  val DatasetName = "twitter.US_literacy"
  val TimeFieldName = "create_at"
  val LiteracySchema: Schema = new Schema("literacy",
    Seq(
      TimeField(TimeFieldName),
      NumberField("id"),
      NumberField("stateID")),
    Seq(
      NumberField("literacy")
    ),
    Seq("id"),
    TimeFieldName)
}
