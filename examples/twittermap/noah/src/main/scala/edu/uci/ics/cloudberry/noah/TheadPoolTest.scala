package edu.uci.ics.cloudberry.noah

import java.io.File
import java.util.Date
import java.util.concurrent.{ExecutorService, Executors}

import edu.uci.ics.cloudberry.gnosis.{CityLevel, CountyLevel, StateLevel, USGeoGnosis}
import edu.uci.ics.cloudberry.util.Profile.profile

object TheadPoolTest {
  def main(args: Array[String]): Unit = {
    val threadPool: ExecutorService = Executors.newFixedThreadPool(5)
    var lc = 0
    //
    val shapeMap = Map.apply(StateLevel -> new File("/root/cloudberry/examples/twittermap/web/public/data/state.json"),
      CountyLevel -> new File("/root/cloudberry/examples/twittermap/web/public/data/county.json"),
      CityLevel -> new File("/root/cloudberry/examples/twittermap/web/public/data/city.json"))
    val usGeoGnosis = profile("load shapes")(new USGeoGnosis(shapeMap))
    //
    val now = new Date()
    val a = now.getTime
    for (ln <- scala.io.Source.fromFile("/root/cloudberry/examples/twittermap/noah/target/scala-2.11/test-classes/sample.json").getLines()) {
      lc = lc + 1
      threadPool.execute(new ThreadDemo(ln, usGeoGnosis))
      if (lc % 100000 == 0)
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

class ThreadDemo(ln: String, usGeoGnosis: USGeoGnosis) extends Runnable {
  override def run() {
        println(TwitterJSONTagToADM.tagOneTweet(ln, usGeoGnosis)+ln)
  }
}
