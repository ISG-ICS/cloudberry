package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._

/**
  * Created by nishadgurav on 1/19/17.
  */
object PopulationDataStore {
  val DatasetName = "twitter.US_population"
  val TimeFieldName = "create_at"
  val PopulationSchema: Schema = new Schema("population",
                                            Seq(
                                              TimeField(TimeFieldName),
                                              NumberField("id"),
                                              NumberField("stateID")),
                                            Seq(
                                              NumberField("population")
                                            ),
                                            Seq("id"),
                                            TimeFieldName)
}
