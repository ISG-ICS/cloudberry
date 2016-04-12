package edu.uci.ics.cloudberry.oracle

trait IEntity {

  /**
    * The hierarchy level of the current entity. [0, +Inf), smaller value means higher level
    *
    * @return
    */
  def level: Int

  def key: Long

  def parentKey: Long
}

trait IGeoJSONEntity extends IEntity {

  def geoID: String

  def name: String

  override val hashCode: Int = geoID.hashCode()

  override def equals(obj: scala.Any): Boolean =
    obj.isInstanceOf[IGeoJSONEntity] && obj.asInstanceOf[IGeoJSONEntity].geoID.equals(geoID)
}

