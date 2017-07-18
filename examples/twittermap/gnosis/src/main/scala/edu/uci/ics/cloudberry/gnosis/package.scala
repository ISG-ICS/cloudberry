package edu.uci.ics.cloudberry

import java.io.File
import java.nio.charset.{Charset, CodingErrorAction}

package object gnosis {

  class UnknownEntityException(entity: IEntity) extends RuntimeException("unknown entity:" + entity)

  type TypeLevel = Int
  val StateLevel: TypeLevel = 1
  val CountyLevel: TypeLevel = 2
  val CityLevel: TypeLevel = 3

  val BoroLevel: TypeLevel = 4
  val NeighborLevel: TypeLevel = 5

  val OrderedLevels: Seq[TypeLevel] = Seq(StateLevel, CountyLevel, CityLevel)

  var NYLevels: Seq[TypeLevel] = Seq(BoroLevel, NeighborLevel)

  def loadSmallJSONFile(file: File): String = {
    val decoder = Charset.forName("UTF-8").newDecoder()
    decoder.onMalformedInput(CodingErrorAction.IGNORE)
    scala.io.Source.fromFile(file)(decoder).getLines().mkString("\n")
  }

}
