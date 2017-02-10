package edu.uci.ics.cloudberry.zion.common

import org.specs2.mutable.Specification

class ConfigTest extends Specification {

  "ConfigTest" should {
    "parseTimePair" in {
      import scala.concurrent.duration._
      Config.parseTimePair("42 min") must_==(42 minutes)
      Config.parseTimePair("43 minute") must_==(43 minutes)
      Config.parseTimePair("84 minutes") must_==(84 minutes)
    }
    "parseFrameLengthLimit" in {
      Config.parseFrameLengthLimit("20 B") must_==(8 * MemorySize.MB)
      Config.parseFrameLengthLimit("0 KB") must_==(8 * MemorySize.MB)
      Config.parseFrameLengthLimit("0.9 KB") must_==(8 * MemorySize.MB)
      Config.parseFrameLengthLimit("1 KB") must_==(1 * MemorySize.KB)
      Config.parseFrameLengthLimit("16 MB") must_==(16 * MemorySize.MB)
      Config.parseFrameLengthLimit("16MB") must_==(16 * MemorySize.MB)
      Config.parseFrameLengthLimit("1.5 GB") must_==(1.5 * MemorySize.GB)
      Config.parseFrameLengthLimit("2 GB") must_==(Int.MaxValue)
      Config.parseFrameLengthLimit("3 GB") must_==(8 * MemorySize.MB)
      Config.parseFrameLengthLimit("5 TB") must_==(8 * MemorySize.MB)
    }
  }
}
