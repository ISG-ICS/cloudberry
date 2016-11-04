package controllers

import java.io.File

import org.specs2.mutable.Specification

/**
  * Created by zongh on 10/26/2016.
  */
class ApplicationTest extends Specification {

  "application" should {
    "load the city data from a file" in {
      val cities = Application.loadCity(new File("neo/public/data/city.json"))
      cities.size must_== 29834
    }

    "calculate centroid" in {
      val cities = Application.loadCity(new File("neo/public/data/city.sample.json"))
      cities.size must_== 1006
      (cities.apply(0) \ "centroidX").as[Double] must_== -176.6287565
      (cities.apply(0) \ "centroidY").as[Double] must_== 51.879214000000005
    }
  }
}
