package edu.uci.ics.cloudberry.gnosis

import java.io._

import edu.uci.ics.cloudberry.gnosis.USAnnotationHelper.HelperProp
import edu.uci.ics.cloudberry.util.Profile._
import play.api.libs.json.{JsObject, Json}

import scala.collection.mutable

object USHierarchyBuilder {


  val StateJsonPath = "StatePath"
  val CountyJsonPath = "CountyPath"
  val CityJsonPath = "CityPath"

  val usage =
    """
      |Usage: USHierarchyBuilder -state /path/to/state.json -county /path/to/county.json -city /path/to/city.json
      |It will generate the spatial relationship between state, county, and cities.
    """.stripMargin

  def parseOption(map: OptionMap, list: List[String]) {
    list match {
      case Nil =>
      case "-h" :: tail => System.err.println(usage); System.exit(0)
      case "-state" :: value :: tail => parseOption(map += (StateJsonPath -> value), tail)
      case "-county" :: value :: tail => parseOption(map += (CountyJsonPath -> value), tail)
      case "-city" :: value :: tail => parseOption(map += (CityJsonPath -> value), tail)
      case option :: tail => System.err.println("unknown option:" + option); System.err.println(usage); System.exit(1);
    }
  }

  type OptionMap = mutable.Map[String, Any]

  val stateIndex = new USGeoJSONIndex()
  val countyIndex = new USGeoJSONIndex()
  val cityIndex = new USGeoJSONIndex()
  val cityToCountyMap = mutable.Map.empty[Int, Int]

  def main(args: Array[String]) = {
    val config: OptionMap = mutable.Map.empty[String, Any]
    parseOption(config, args.toList)
    profile("loadShape")(loadShapes(config))
    profile("parseRelation")(parseRelation)
    profile("writeRelation")(writeRelation)
  }

  def loadShapes(config: OptionMap): Unit = {
    Seq((StateJsonPath, stateIndex),
        (CountyJsonPath, countyIndex),
        (CityJsonPath, cityIndex)).foreach {
      case (key: String, index: USGeoJSONIndex) => config.get(key) match {
        case Some(path: String) =>
          USGeoGnosis.loadShape(path, index, Seq.empty[HelperProp])
        case _ => System.err.print(usage); throw new IllegalArgumentException(s"$key is missing")
      }
    }
  }


  def parseRelation(): Unit = {
    profile("parse city") {
      cityIndex.entities.foreach(entity => {
        val city = entity.asInstanceOf[USCityEntity]
        val countyID = findCounty(city)
        cityToCountyMap += city.cityID -> countyID
      })
    }
  }

  def findCounty(city: USCityEntity): Int = {
    countyIndex.search(city.geometry).foldLeft((0, 0.0))((p, entity) => {
      val area = entity.geometry.intersection(city.geometry).getArea
      if (area > p._2) (entity.asInstanceOf[USCountyEntity].countyID, area)
      else p
    })._1
  }

  def writeRelation(): Unit = {
    writeGeoProperty("state.json", annotate(stateIndex.entities))
    writeGeoProperty("county.json", annotate(countyIndex.entities))
    writeGeoProperty("city.json", annotate(cityIndex.entities))
  }

  def annotate(entities: Seq[IUSGeoJSONEntity]): Seq[JsObject] = {
    val Unknown = "Unknown"
    def getStateName(stateID: Int): String = stateIndex.entities.find(_.stateID == stateID).map(_.name).getOrElse(Unknown)
    def getCountyName(stateID: Int, countyID: Int): String =
      countyIndex.entities.map(_.asInstanceOf[USCountyEntity]).
        find(county => county.stateID == stateID && county.countyID == countyID).map(_.name).getOrElse(Unknown)

    entities.sortBy(_.geoID).map(entity => entity match {
      case e: USStateEntity =>
        Json.obj("geoID" -> e.geoID, "stateID" -> e.stateID, "name" -> e.name, "LSAD" -> e.LSAD, "area" -> e.area)
      case e: USCountyEntity =>
        Json.obj("geoID" -> e.geoID,
                 "stateID" -> e.stateID,
                 "stateName" -> getStateName(e.stateID),
                 "countyID" -> e.countyID,
                 "name" -> e.name,
                 "LSAD" -> e.LSAD,
                 "area" -> e.area)
      case e: USCityEntity =>
        val countyID: Int = cityToCountyMap.get(e.cityID).getOrElse(0)
        Json.obj("geoID" -> e.geoID,
                 "stateID" -> e.stateID,
                 "stateName" -> getStateName(e.stateID),
                 "countyID" -> countyID,
                 "countyName" -> getCountyName(e.stateID, countyID),
                 "cityID" -> e.cityID,
                 "name" -> e.name,
                 "LSAD" -> e.LSAD,
                 "area" -> e.area)
    })
  }

  def writeGeoProperty(filePath: String, jsons: Seq[JsObject]): Unit = {
    val file = new File(filePath)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write("[ " + jsons.head.toString)
    jsons.tail.foreach {
      json =>
        bw.newLine()
        bw.write("," + json.toString)
    }
    bw.newLine()
    bw.write(']')
    bw.close()
  }

}

