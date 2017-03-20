package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema.{NumberField, Schema, TimeField}

/**
  * Represents a 'Literacy Dataset' which is used to test parsing of lookup queries.
  * It will hold literacy rate of each state. We can lookup the literacy of the state for each tweet in the
  * twitter dataset.
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
                                          Seq(NumberField("id")),
                                          TimeField(TimeFieldName))
}
