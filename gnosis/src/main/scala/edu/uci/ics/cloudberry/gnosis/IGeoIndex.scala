package edu.uci.ics.cloudberry.gnosis

import com.vividsolutions.jts.geom.{Envelope, Geometry}
import com.vividsolutions.jts.index.strtree.STRtree
import org.wololo.geojson.{Feature, FeatureCollection, GeoJSONFactory}
import org.wololo.jts2geojson.{GeoJSONReader, GeoJSONWriter}

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

trait IGeoIndex {

  def search(geometry: Geometry): Seq[IEntity]

  def search(envelope: Envelope): Seq[IEntity]
}

class USGeoJSONIndex() extends IGeoIndex {
  private val index: STRtree = new STRtree()
  val entities: ArrayBuffer[IUSGeoJSONEntity] = new ArrayBuffer[IUSGeoJSONEntity]()

  /**
    * Load GeoJson. It may get called many times, but once calls search() methods it can not load any more.
    *
    * @param geoJsonString
    */
  def loadShape(geoJsonString: String)(implicit builder: (Map[String, AnyRef], Geometry) => IUSGeoJSONEntity): Unit = {
    val geoJSONReader = new GeoJSONReader()
    val featureCollection: FeatureCollection = GeoJSONFactory.create(geoJsonString).asInstanceOf[FeatureCollection]
    featureCollection.getFeatures.foreach { f: Feature =>
      val geometry: Geometry = geoJSONReader.read(f.getGeometry)
      entities += builder(f.getProperties.asScala.toMap, geometry)
      index.insert(geometry.getEnvelopeInternal, entities.size - 1)
    }
  }

  override def search(geometry: Geometry): Seq[IUSGeoJSONEntity] = {
    search(geometry.getEnvelopeInternal).filter(_.geometry.intersects(geometry))
  }

  override def search(envelope: Envelope): Seq[IUSGeoJSONEntity] = {
    index.query(envelope).asScala.map(item => entities(item.asInstanceOf[Int]))
  }
}

class NYGeoJSONIndex() extends IGeoIndex {
  private val index: STRtree = new STRtree()
  val entities: ArrayBuffer[INYGeoJSONEntity] = new ArrayBuffer[INYGeoJSONEntity]()

  /**
    * Load GeoJson. It may get called many times, but once calls search() methods it can not load any more.
    *
    * @param geoJsonString
    */
  def loadShape(geoJsonString: String)(implicit builder: (Map[String, AnyRef], Geometry) => INYGeoJSONEntity): Unit = {
    val geoJSONReader = new GeoJSONReader()
    val featureCollection: FeatureCollection = GeoJSONFactory.create(geoJsonString).asInstanceOf[FeatureCollection]
    featureCollection.getFeatures.foreach { f: Feature =>
      val geometry: Geometry = geoJSONReader.read(f.getGeometry)
      entities += builder(f.getProperties.asScala.toMap, geometry)
      index.insert(geometry.getEnvelopeInternal, entities.size - 1)
    }
  }

  override def search(geometry: Geometry): Seq[INYGeoJSONEntity] = {
    search(geometry.getEnvelopeInternal).filter(_.geometry.intersects(geometry))
  }

  override def search(envelope: Envelope): Seq[INYGeoJSONEntity] = {
    index.query(envelope).asScala.map(item => entities(item.asInstanceOf[Int]))
  }
}