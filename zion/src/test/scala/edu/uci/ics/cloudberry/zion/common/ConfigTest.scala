package edu.uci.ics.cloudberry.zion.common

import org.specs2.mutable.Specification

class ConfigTest extends Specification {

  "ConfigTest" should {
    "parseTimePair" in {
      import scala.concurrent.duration._
      Config.parseTimePair("42 min") must_== (42 minutes)
      Config.parseTimePair("43 minute") must_== (43 minutes)
      Config.parseTimePair("84 minutes") must_== (84 minutes)
    }
  }
}
