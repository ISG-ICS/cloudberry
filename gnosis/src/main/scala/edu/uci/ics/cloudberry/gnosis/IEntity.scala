package edu.uci.ics.cloudberry.gnosis

trait IEntity {

  /**
    * The hierarchy level of the current entity. [0, +Inf), smaller value means higher level
    *
    * @return
    */
  def level: TypeLevel

  /**
    * The level of the parent entity. Some of the entity may go to the grandparent directly.
    * @return
    */
  def parentLevel: TypeLevel

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

