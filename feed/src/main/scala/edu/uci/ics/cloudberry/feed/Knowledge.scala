package actors

import java.io._
import java.nio.charset.{Charset, CodingErrorAction}

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.index.strtree.STRtree
import models.Rectangular
import org.wololo.geojson.{Feature, FeatureCollection, GeoJSONFactory}
import org.wololo.jts2geojson.GeoJSONReader

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object Knowledge {

  val StateShapePath = "public/data/state.shape.20m.json"
  val CountyShapePath = "public/data/county.shape.20m.json"
  val StateAbbrPath = "public/data/states_hash.json"
  val CitiesPath = "public/data/cities"
  val IndexState = new STRtree()
  val IndexCounty = new STRtree()
  val IndexCities = new STRtree()
  var StateAbbrMap = Map.empty[String, String]
  var ArrayStateProp: ArrayBuffer[GeoJSONProperty] = new ArrayBuffer[GeoJSONProperty](60)
  var ArrayCountyProp: ArrayBuffer[GeoJSONProperty] = new ArrayBuffer[GeoJSONProperty](3230)
  var ArrayCityProp: ArrayBuffer[GeoJSONProperty] = new ArrayBuffer[GeoJSONProperty](30000)

  def loadShape(inputStream: InputStream, index: STRtree, buffer: ArrayBuffer[GeoJSONProperty]) = {
    val geoJSONReader = new GeoJSONReader()
    val decoder = Charset.forName("UTF-8").newDecoder()
    decoder.onMalformedInput(CodingErrorAction.IGNORE)
    val textJson = scala.io.Source.fromInputStream(inputStream)(decoder).getLines().mkString("\n")
    val featureCollection: FeatureCollection = GeoJSONFactory.create(textJson).asInstanceOf[FeatureCollection]
    featureCollection.getFeatures.foreach { f: Feature =>
      val geometry: Geometry = geoJSONReader.read(f.getGeometry)
      buffer += GeoJSONProperty(f.getProperties.asScala.toMap, geometry)
      index.insert(geometry.getEnvelopeInternal, buffer.size - 1)
    }
    buffer
  }

  def loadStringMapping(inputStream: InputStream): Map[String, String] = {
    Json.parse(inputStream).as[Map[String, String]](mapFormatter)
  }

  def writeGeoProperty(filePath: String, arrayBuffer: ArrayBuffer[GeoJSONProperty]): Unit = {
    val file = new File(filePath)
    val bw = new BufferedWriter(new FileWriter(file))
    val jsons = arrayBuffer.sortBy(_.geoID).map(_.toJson)
    bw.write("[ " + jsons.head.toString)
    jsons.tail.foreach { json =>
      bw.newLine()
      bw.write("," + json.toString)
    }
    bw.newLine()
    bw.write(']')
    bw.close()
  }

  def geoTag(area: Rectangular, level: String): Seq[String] = {
    val (index: STRtree, array: ArrayBuffer[GeoJSONProperty]) =
      level.toLowerCase match {
        case "state" => (IndexState, ArrayStateProp)
        case "county" => (IndexCounty, ArrayCountyProp)
        case "city" => (IndexCities, ArrayCityProp)
        case other => throw new UnknownSpatialLevelException(other)
      }
    // it may returns some false positives, but it should be fine in our application.
    // in which case it just fetch more results.
    index.query(area.getEnvelopInternal).asScala.map(item => array(item.asInstanceOf[Int]).getAsterixKey)
  }

  def geoTag(geometry: Geometry, index: STRtree, array: ArrayBuffer[GeoJSONProperty]): Seq[GeoJSONProperty] = {
    index.query(geometry.getEnvelopeInternal).asScala
      .map(item => array(item.asInstanceOf[Int])).filter(_.geometry.intersects(geometry))
  }

  class UnknownSpatialLevelException(level: String) extends IllegalArgumentException(s"unknown spatial level: $level")

  sealed trait GeoJSONProperty {
    def getAsterixKey: String

    def getKey: Long

    def geoID: String

    def stateID: Int

    def name: String

    def LSAD: String

    def area: Double

    def geometry: Geometry

    def toJson: JsObject

    override def hashCode(): Int = geoID.hashCode

    override def equals(obj: scala.Any): Boolean =
      obj.isInstanceOf[GeoJSONProperty] && obj.asInstanceOf[GeoJSONProperty].geoID.eq(geoID)
  }

  object GeoJSONProperty {
    val GEO_ID = "GEO_ID"
    val STATE = "STATE"
    val NAME = "NAME"
    val LSAD = "LSAD"
    val AREA = "CENSUSAREA"
    val COUNTY = "COUNTY"
    val PLACEEFP = "PLACEFP"

    def findCounty(stateID: Int, geometry: Geometry): Int = {
      val counties: Seq[CountyProperty] =
        geoTag(geometry, IndexCounty, ArrayCountyProp).filter(_.stateID == stateID).map(_.asInstanceOf[CountyProperty])
      var id = 0
      var maxIntersectArea = 0.0
      counties.foreach { county =>
        if (county.geometry.contains(geometry)) return county.countyID
        val intersectArea = county.geometry.intersection(geometry).getArea
        if (maxIntersectArea < intersectArea) {
          maxIntersectArea = intersectArea
          id = county.countyID
        }
      }
      id
    }

    def apply(map: Map[String, AnyRef], geometry: Geometry): GeoJSONProperty = {
      map.get(PLACEEFP) match {
        case Some(obj) =>
          import CityProperty._
          CityProperty(
            geoID = map.get(GEOID).get.asInstanceOf[String],
            stateID = map.get(STATEFP).get.asInstanceOf[String].toInt,
            countyID = findCounty(map.get(STATEFP).get.asInstanceOf[String].toInt, geometry),
            cityID = map.get(CITYID).get.asInstanceOf[String].toInt,
            name = map.get(NAME).get.asInstanceOf[String],
            LSAD = map.get(LSAD).get.asInstanceOf[String],
            landArea = map.get(ALADN).get.asInstanceOf[Double],
            waterArea = map.get(AWATER).get.asInstanceOf[Double],
            geometry
          )
        case None =>
          map.get(COUNTY) match {
            case Some(obj) =>
              CountyProperty(
                geoID = map.get(GEO_ID).get.asInstanceOf[String],
                stateID = map.get(STATE).get.asInstanceOf[String].toInt,
                countyID = map.get(COUNTY).get.asInstanceOf[String].toInt,
                name = map.get(NAME).get.asInstanceOf[String],
                LSAD = map.get(LSAD).get.asInstanceOf[String],
                area = map.get(AREA).get.asInstanceOf[Double],
                geometry
              )
            case None => StateProperty(
              geoID = map.get(GEO_ID).get.asInstanceOf[String],
              stateID = map.get(STATE).get.asInstanceOf[String].toInt,
              name = map.get(NAME).get.asInstanceOf[String],
              LSAD = map.get(LSAD).get.asInstanceOf[String],
              area = map.get(AREA).get.asInstanceOf[Double],
              geometry
            )
          }
      }
    }

  }

  case class StateProperty(geoID: String,
                           stateID: Int,
                           name: String,
                           LSAD: String,
                           area: Double,
                           geometry: Geometry) extends GeoJSONProperty {
    val abbr = StateAbbrMap.getOrElse(name, "UN")
    override val getAsterixKey: String = abbr
    override val getKey: Long = stateID

    override def toJson: JsObject =
      Json.obj("geoID" -> geoID, "stateID" -> stateID, "name" -> name, "LSAD" -> LSAD, "area" -> area)
  }

  case class CountyProperty(geoID: String,
                            stateID: Int,
                            countyID: Int,
                            name: String, LSAD:
                            String, area: Double,
                            geometry: Geometry) extends GeoJSONProperty {
    val stateName: String = ArrayStateProp.filter(_.stateID == stateID).headOption.map(_.name).getOrElse("Unknown")
    override val getAsterixKey: String = stateName + "-" + name
    override val getKey: Long = stateID * 1000 + countyID

    override def toJson: JsObject =
      Json.obj("geoID" -> geoID, "stateID" -> stateID, "stateName" -> stateName, "countyID" -> countyID,
               "name" -> name, "LSAD" -> LSAD, "area" -> area)
  }

  case class CityProperty(geoID: String,
                          stateID: Int,
                          countyID: Int,
                          cityID: Int,
                          name: String,
                          LSAD: String,
                          landArea: Double,
                          waterArea: Double,
                          geometry: Geometry) extends GeoJSONProperty {
    val area = landArea + waterArea
    val stateName: String = ArrayStateProp.filter(_.stateID == stateID).headOption.map(_.name).getOrElse("Unknown")
    val countyName: String = {
      if (countyID <= 0) "Unknown"
      else ArrayCountyProp.filter(prop => {
        val cp = prop.asInstanceOf[CountyProperty]
        cp.countyID == countyID && cp.stateID == stateID
      }).apply(0).name
    }
    override val getAsterixKey: String = s"$stateName-$countyName-$name"
    override val getKey: Long = stateID * 1000 * 100000 + countyID * 100000 + cityID

    override def toJson: JsObject =
      Json.obj("geoID" -> geoID, "stateID" -> stateID, "stateName" -> stateName, "countyID" -> countyID,
               "countyName" -> countyName, "cityID" -> cityID, "name" -> name, "LSAD" -> LSAD, "area" -> area)
  }

  object CityProperty {
    val STATEFP = "STATEFP"
    val PLACEEFP = "PLACEEFP"
    val GEOID = "AFFGEOID"
    val CITYID = "GEOID"
    val ALADN = "ALAND"
    val AWATER = "AWATER"
  }

  def decompose(entity: String): Seq[String] = {
    return Seq("CA-LA", "CA-Orange County")
  }

  implicit val mapFormatter: Format[Map[String, String]] = {
    new Format[Map[String, String]] {
      override def writes(m: Map[String, String]): JsValue = {
        val fields: Seq[(String, JsValue)] = m.map {
          case (k, v) => k -> JsString(v)
        }(collection.breakOut)
        JsObject(fields)
      }

      override def reads(json: JsValue): JsResult[Map[String, String]] = {
        JsSuccess {
          json.asOpt[JsObject] match {
            case Some(obj) => {
              val builder = Map.newBuilder[String, String]
              val each = obj.fields.toMap.mapValues { v =>
                v.asOpt[JsString] match {
                  case Some(str) => str.value
                  case None => ""
                }
              }
              each.foreach { e => builder += e._1 -> e._2 }
              builder.result()
            }
            case None => Map.empty[String, String]
          }
        }
      }

    }
  }
}

