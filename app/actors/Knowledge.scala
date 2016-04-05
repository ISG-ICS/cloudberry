package actors

import com.esri.core.geometry.Polygon
import models.Rectangular
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

object Knowledge {
  def loadFromDB = Future {}

  def geoTag(area: Rectangular, level: String): Seq[String] = {
    return Seq(
      "AL",
      "AK",
      "AS",
      "AZ",
      "AR",
      "CA",
      "CO",
      "CT",
      "DE",
      "DC",
      "FM",
      "FL",
      "GA",
      "GU",
      "HI",
      "ID",
      "IL",
      "IN",
      "IA",
      "KS",
      "KY",
      "LA",
      "ME",
      "MH",
      "MD",
      "MA",
      "MI",
      "MN",
      "MS",
      "MO",
      "MT",
      "NE",
      "NV",
      "NH",
      "NJ",
      "NM",
      "NY",
      "NC",
      "ND",
      "MP",
      "OH",
      "OK",
      "OR",
      "PW",
      "PA",
      "PR",
      "RI",
      "SC",
      "SD",
      "TN",
      "TX",
      "UT",
      "VT",
      "VI",
      "VA",
      "WA",
      "WV",
      "WI",
      "WY")
  }

  def decompose(entity: String): Seq[String] = {
    return Seq("CA-LA", "CA-Orange County")
  }

  // dataset information
  // dataverse
  // field to indexed on . etc.
}

