package edu.uci.ics.cloudberry

import java.nio.charset.{Charset, CodingErrorAction}

package object gnosis {

  class UnknownEntityException(entity: IEntity) extends RuntimeException("unknown entity:" + entity)

  type TypeLevel = Int

  def loadSmallJSONFile(fileName: String): String = {
    val decoder = Charset.forName("UTF-8").newDecoder()
    decoder.onMalformedInput(CodingErrorAction.IGNORE)
    scala.io.Source.fromFile(fileName)(decoder).getLines().mkString("\n")
  }

}
