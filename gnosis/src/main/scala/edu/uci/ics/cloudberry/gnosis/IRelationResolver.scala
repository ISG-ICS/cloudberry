package edu.uci.ics.cloudberry.gnosis

trait IRelationResolver {

  def getChildren(entity: IEntity): Seq[IEntity]

  def getParent(entity: IEntity): Option[IEntity]
}

