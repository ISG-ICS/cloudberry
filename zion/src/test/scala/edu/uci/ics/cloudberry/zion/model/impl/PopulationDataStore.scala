package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._

/**
  * Represents a 'Population Dataset' which is used to test parsing of lookup queries.
  * It will hold population of each state. We can lookup the population of the state for each tweet in the
  * twitter dataset.
  */
object PopulationDataStore {
  val DatasetName = "twitter.US_population"
  val PopulationSchema = LookupSchema("population",
                                      Seq(
                                        NumberField("id"),
                                        NumberField("stateID")
                                      ),
                                      Seq(
                                        NumberField("population")
                                      ),
                                      Seq(
                                        NumberField("id")
                                      )
  )
}
