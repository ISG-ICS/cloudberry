package edu.uci.ics.cloudberry.noah
import java.io.File
import edu.uci.ics.cloudberry.gnosis._
import edu.uci.ics.cloudberry.noah.adm.TaxiTrip
import edu.uci.ics.cloudberry.util.Profile._

import scala.collection.mutable
import scala.io.Source

object NYTaxiTripJSONTagToADM {

  val shapeMap = mutable.Map.empty[TypeLevel, String]

  val srcPath = new StringBuilder
  val usage =
    """
      |Usage: USHierarchyBuilder -b /path/to/borough.json -n /path/to/neighborhood.json
      |-f /path/to/csv/file
      |It will read taxi trip csv file, geoTag borough/neighborhood information, and output an ADM file
    """.stripMargin

  def parseOption(list: List[String]) {
    list match {
      case Nil =>
      case "-h" :: tail => System.err.println(usage); System.exit(0)
      case "-b" :: value :: tail => shapeMap += BoroLevel -> value; parseOption(tail)
      case "-n" :: value :: tail => shapeMap += NeighborLevel -> value; parseOption(tail)
      case "-f" :: value :: tail => srcPath ++= value; parseOption(tail)
      case option :: tail => System.err.println("unknown option:" + option); System.err.println(usage); System.exit(1);
    }
  }

  def main(args: Array[String]): Unit = {
    parseOption(args.toList)

    val nyGeoGnosis = profile("loading resource") {
      new NewYorkGnosis(shapeMap.mapValues(new File(_)).toMap)
    }

    TaxiTrip.toADMFile(srcPath.toString, nyGeoGnosis)

  }
}
