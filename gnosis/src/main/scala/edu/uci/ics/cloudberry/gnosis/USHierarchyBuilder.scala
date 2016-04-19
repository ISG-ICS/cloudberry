package edu.uci.ics.cloudberry.gnosis

import java.io._

import com.vividsolutions.jts.geom.Geometry
import edu.uci.ics.cloudberry.util.Profile._
import org.wololo.geojson.Feature
import org.wololo.jts2geojson.GeoJSONWriter

import scala.collection.JavaConverters._
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
          USGeoGnosis.loadShape(new File(path), index)(RawEntityBuilder.apply)
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
      if (entity.asInstanceOf[USCountyEntity].stateID != city.stateID) {
        p
      } else {
        val area = entity.geometry.intersection(city.geometry).getArea
        if (area > p._2) (entity.asInstanceOf[USCountyEntity].countyID, area)
        else p
      }
    })._1
  }

  def writeRelation(): Unit = {
    writeGeoProperty("state.json", annotate(stateIndex.entities))
    writeGeoProperty("county.json", annotate(countyIndex.entities))
    writeGeoProperty("city.json", annotate(cityIndex.entities))
  }

  def annotate(entities: Seq[IUSGeoJSONEntity]): Seq[String] = {
    val Unknown = "Unknown"
    def getStateName(stateID: Int): String = stateIndex.entities.find(_.stateID == stateID).map(_.name).getOrElse(Unknown)
    def getCountyName(stateID: Int, countyID: Int): String =
      countyIndex.entities.map(_.asInstanceOf[USCountyEntity]).
        find(county => county.stateID == stateID && county.countyID == countyID).map(_.name).getOrElse(Unknown)

    entities.sortBy(_.geoID).map(entity => entity match {
      case e: USStateEntity =>
        writeGeoJsonFeature(e.geometry, e.toPropertyMap)
      case e: USCountyEntity =>
        writeGeoJsonFeature(e.geometry, e.copy(stateName = getStateName(e.stateID),
                                               countyID = e.stateID * 1000 + e.countyID).toPropertyMap)
      case e: USCityEntity =>
        val countyID: Int = cityToCountyMap.get(e.cityID).getOrElse(0)
        writeGeoJsonFeature(e.geometry,
                            e.copy(countyID = e.stateID * 1000 + countyID,
                                   stateName = getStateName(e.stateID),
                                   countyName = getCountyName(e.stateID, countyID)).toPropertyMap)
    })
  }


  val writer = new GeoJSONWriter()

  //TODO convert to stream to save a big chunk of memory
  def writeGeoJsonFeature(geometry: Geometry, propertyMap: Map[String, AnyRef]): String = {
    new Feature(writer.write(geometry), propertyMap.asJava).toString
  }

  def writeGeoProperty(filePath: String, geojsonString: Seq[String]): Unit = {
    val file = new File(filePath)
    val bw = new BufferedWriter(new FileWriter(file))
    bw.write("{ \"type\": \"FeatureCollection\",")
    bw.newLine()
    bw.write("\"features\": [")
    bw.newLine()
    bw.write(geojsonString.head.toString)
    geojsonString.tail.foreach {
      json =>
        bw.newLine()
        bw.write("," + json.toString)
    }
    bw.newLine()
    bw.write(']')
    bw.newLine()
    bw.write('}')
    bw.newLine()
    bw.close()
  }

}

object RawEntityBuilder {
  val GEO_ID = "GEO_ID"
  val STATE = "STATE"
  val NAME = "NAME"
  val LSAD = "LSAD"
  val AREA = "CENSUSAREA"
  val COUNTY = "COUNTY"
  val PLACEEFP = "PLACEFP"
  val PENDING = "pending"

  val STATEFP = "STATEFP"
  val GEOID = "AFFGEOID"
  val CITYID = "GEOID"
  val ALADN = "ALAND"
  val AWATER = "AWATER"

  def apply(map: Map[String, AnyRef], geometry: Geometry): IUSGeoJSONEntity = {
    map.get(PLACEEFP) match {
      case Some(obj) =>
        USCityEntity(
          geoID = map.get(GEOID).get.asInstanceOf[String],
          stateID = map.get(STATEFP).get.asInstanceOf[String].toInt,
          stateName = PENDING,
          countyID = 0,
          countyName = PENDING,
          cityID = map.get(CITYID).get.asInstanceOf[String].toInt,
          name = map.get(NAME).get.asInstanceOf[String],
          landArea = map.get(ALADN).get.asInstanceOf[Double],
          waterArea = map.get(AWATER).get.asInstanceOf[Double],
          geometry
        )
      case None =>
        map.get(COUNTY) match {
          case Some(obj) =>
            USCountyEntity(
              geoID = map.get(GEO_ID).get.asInstanceOf[String],
              stateID = map.get(STATE).get.asInstanceOf[String].toInt,
              stateName = PENDING,
              countyID = map.get(COUNTY).get.asInstanceOf[String].toInt,
              name = map.get(NAME).get.asInstanceOf[String],
              area = map.get(AREA).get.asInstanceOf[Double],
              geometry
            )
          case None => USStateEntity(
            geoID = map.get(GEO_ID).get.asInstanceOf[String],
            stateID = map.get(STATE).get.asInstanceOf[String].toInt,
            name = map.get(NAME).get.asInstanceOf[String],
            area = map.get(AREA).get.asInstanceOf[Double],
            geometry
          )
        }
    }
  }
}
