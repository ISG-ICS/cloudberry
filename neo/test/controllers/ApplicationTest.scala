package controllers

import java.io.File

import org.specs2.mutable.Specification

/**
  * Created by zongh on 10/26/2016.
  */
class ApplicationTest extends Specification {

  "application" should {
    "load the city data from a file" in {
      val cities = Application.loadCity(new File("C:\\Users\\zongh\\OneDrive\\Documents\\UCI\\cloudberry\\cloudberry\\neo\\public\\data\\city.json"))
      cities.size must_== 29834
    }
  }
}
