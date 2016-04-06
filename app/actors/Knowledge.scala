package actors

import java.io.InputStream

import com.vividsolutions.jts.geom.Geometry
import com.vividsolutions.jts.index.strtree.STRtree
import models.Rectangular
import org.wololo.geojson.{Feature, FeatureCollection, GeoJSONFactory}
import org.wololo.jts2geojson.GeoJSONReader
import play.api.Environment
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import utils.Profile

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

object Knowledge {

  val StateShapePath = "public/data/state.shape.20m.json"
  val CountyShapePath = "public/data/county.shape.20m.json"
  val StateAbbrPath = "public/data/states_hash.json"
  val StateIndex = new STRtree()
  val CountyIndex = new STRtree()
  var StateAbbrMap = Map.empty[String, String]
  var ArrayStateProp: ArrayBuffer[GeoJSONProperty] = null
  var ArrayCountyProp: ArrayBuffer[GeoJSONProperty] = null

  def loadShape(inputStream: InputStream, index: STRtree): ArrayBuffer[GeoJSONProperty] = {
    val geoJSONReader = new GeoJSONReader()
    val textJson = scala.io.Source.fromInputStream(inputStream, "UTF-8").getLines().mkString("\n")
    val featureCollection: FeatureCollection = GeoJSONFactory.create(textJson).asInstanceOf[FeatureCollection]
    val buffer = new ArrayBuffer[GeoJSONProperty](featureCollection.getFeatures.length)
    featureCollection.getFeatures.foreach { f: Feature =>
      val geometry: Geometry = geoJSONReader.read(f.getGeometry)
      buffer += GeoJSONProperty(f.getProperties.asScala.toMap)
      index.insert(geometry.getEnvelopeInternal, buffer.size - 1)
    }
    buffer
  }

  def loadStringMapping(inputStream: InputStream): Map[String, String] = {
    Json.parse(inputStream).as[Map[String, String]](mapFormatter)
  }

  def loadResources(playENV: Environment): Future[Unit] = Future {
    StateAbbrMap = loadStringMapping(playENV.resourceAsStream(StateAbbrPath).get).map(_.swap)
    ArrayStateProp = Profile.profile("load state shape")(loadShape(playENV.resourceAsStream(StateShapePath).get, StateIndex))
    ArrayCountyProp = Profile.profile("load county shape")(loadShape(playENV.resourceAsStream(CountyShapePath).get, CountyIndex))
  }

  def geoTag(area: Rectangular, level: String): Seq[String] = {
    val (index: STRtree, array: ArrayBuffer[GeoJSONProperty]) =
      level.toLowerCase match {
        case "state" => (StateIndex, ArrayStateProp)
        case "county" => (CountyIndex, ArrayCountyProp)
        case other => throw new UnknownSpatialLevelException(other)
      }
    // it may returns some false positives, but it should be fine in our application.
    // in which case it just fetch more results.
    index.query(area.toEnvelop).asScala.map(item => array(item.asInstanceOf[Int]).getAsterixKey)
  }

  class UnknownSpatialLevelException(level: String) extends IllegalArgumentException(s"unknown spatial level: $level")

  sealed trait GeoJSONProperty {
    def getAsterixKey: String

    def geoID: String

    def stateID: Int

    def name: String

    def LSAD: String

    def area: Double

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

    def apply(map: Map[String, AnyRef]): GeoJSONProperty = {
      map.get(COUNTY) match {
        case Some(obj) => CountyProperty(
          geoID = map.get(GEO_ID).get.asInstanceOf[String],
          stateID = map.get(STATE).get.asInstanceOf[String].toInt,
          countyID = map.get(COUNTY).get.asInstanceOf[String].toInt,
          name = map.get(NAME).get.asInstanceOf[String],
          LSAD = map.get(LSAD).get.asInstanceOf[String],
          area = map.get(AREA).get.asInstanceOf[Double]
        )
        case None => StateProperty(
          geoID = map.get(GEO_ID).get.asInstanceOf[String],
          stateID = map.get(STATE).get.asInstanceOf[String].toInt,
          name = map.get(NAME).get.asInstanceOf[String],
          LSAD = map.get(LSAD).get.asInstanceOf[String],
          area = map.get(AREA).get.asInstanceOf[Double]
        )
      }
    }
  }

  case class StateProperty(geoID: String, stateID: Int, name: String, LSAD: String, area: Double) extends GeoJSONProperty {
    val abbr = StateAbbrMap.getOrElse(name, "UN")
    override val getAsterixKey: String = abbr
  }

  case class CountyProperty(geoID: String, stateID: Int, countyID: Int, name: String, LSAD: String, area: Double) extends GeoJSONProperty {
    val stateName = ArrayStateProp.filter(_.stateID == stateID).apply(0).name
    override val getAsterixKey: String = stateName + "-" + name
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

