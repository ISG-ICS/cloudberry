package edu.uci.ics.cloudberry.oracle

import com.vividsolutions.jts.geom.{Envelope, Geometry}
import com.vividsolutions.jts.index.strtree.STRtree
import edu.uci.ics.cloudberry.oracle.USGeoRelationResolver.USHierarchyProp
import org.wololo.geojson.{Feature, FeatureCollection, GeoJSONFactory}
import org.wololo.jts2geojson.GeoJSONReader

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

trait IGeoIndex {

  def loadShape(geoJsonString: String): Unit

  def search(geometry: Geometry): Seq[IGeoJSONEntity]

  def search(envelope: Envelope): Seq[IGeoJSONEntity]
}

class USGeoJSONIndex(val props: Seq[USHierarchyProp]) extends IGeoIndex {
  private val index: STRtree = new STRtree()
  val entities: ArrayBuffer[IUSGeoJSONEntity] = new ArrayBuffer[IUSGeoJSONEntity]()

  /**
    * Load GeoJson. It may get called many times, but once calls search() methods it can not load any more.
    *
    * @param geoJsonString
    */
  override def loadShape(geoJsonString: String): Unit = {
    val geoJSONReader = new GeoJSONReader()
    val featureCollection: FeatureCollection = GeoJSONFactory.create(geoJsonString).asInstanceOf[FeatureCollection]
    featureCollection.getFeatures.foreach { f: Feature =>
      val geometry: Geometry = geoJSONReader.read(f.getGeometry)
      entities += IUSGeoJSONEntity(props, f.getProperties.asScala.toMap, geometry)
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