package actors

import db.AQLConnection
import models.DataSet
import org.joda.time.Interval

/**
  * TODO A special view which doesn't need to specify the keyword.
  * It should generate and utilize the aggregated result to solve the pure Spatial-Temporal related aggregations.
  * @param dataSet
  * @param curTimeRange
  * @param conn
  */
class DBSnapshotActor(override val dataSet: DataSet, @volatile override var curTimeRange: Interval = DBViewActor.DefaultInterval)
                     (implicit override val conn: AQLConnection)
  extends DBViewActor(dataSet, "", curTimeRange){

  // prepare the table if not exists
  def prepare = ???

  def answer = ???
}
