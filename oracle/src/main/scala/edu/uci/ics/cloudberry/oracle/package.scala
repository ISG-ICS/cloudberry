package edu.uci.ics.cloudberry

package object oracle {

  class UnknownEntityException(entity: IEntity) extends RuntimeException("unknown entity:" + entity)

}
