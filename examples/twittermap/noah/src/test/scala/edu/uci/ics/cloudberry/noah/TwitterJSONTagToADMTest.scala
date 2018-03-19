package edu.uci.ics.cloudberry.noah

import java.io.File
import java.util.Date
import edu.uci.ics.cloudberry.gnosis._
import org.scalatest.{FlatSpec, Matchers}
import edu.uci.ics.cloudberry.util.Profile._

class TwitterJSONTagToADMTest extends FlatSpec with Matchers {
  "USGeoGnosis" should "tag the us json file" in {
    val sLoadGeo = new Date()
    val shapeMap = Map.apply(StateLevel -> new File("web/public/data/state.json"),
      CountyLevel -> new File("web/public/data/county.json"),
      CityLevel -> new File("web/public/data/city.json"))
    val usGeoGnosis = profile("load shapes")(new USGeoGnosis(shapeMap))
    val eLoadGeo = new Date()
    println("GeoGnosis Loading: " + (eLoadGeo.getTime - sLoadGeo.getTime).toString + " ms")
    var lc = 0
    val now = new Date()
    val a = now.getTime
    for (ln <- scala.io.Source.fromURL(getClass.getResource("/sample.json")).getLines()) {
      TwitterJSONTagToADM.tagOneTweet(ln, usGeoGnosis)
      lc=lc+1
      if(lc%100000==0)
        println(lc.toString)
    }
    val end = new Date()
    val b = end.getTime
    println("Start and End times, a:" + a.toString + "b:" + b.toString)
    println("Geotaged " + lc.toString + " tweets, time used:" + (b - a).toString + " million seconds.")
    println("Avg. (ms/t):" + ((b - a).toDouble / lc.toDouble).toString())
    println("Avg. tag speed (t/s):" + (lc.toDouble / (b - a) * 1000).toString)
  }

}
