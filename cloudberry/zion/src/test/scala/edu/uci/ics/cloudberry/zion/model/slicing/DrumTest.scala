package edu.uci.ics.cloudberry.zion.model.slicing

import org.specs2.mutable.Specification

class DrumTest extends Specification {

  val minRange = 1
  val drum = new Drum(totalRange = 100, alpha = 0.6, minRange)

  sequential

  "Drum" should {
    "return the min range if history is clear" in {
      val rangeTime = drum.estimate(2000)
      rangeTime.estimateMS must_== Int.MaxValue
      rangeTime.range must_== minRange
    }
    "double the range for the second mini-query" in {
      drum.learn(1, Int.MaxValue, 500)
      val rangeTime = drum.estimate(3500)
      rangeTime.estimateMS must_== Int.MaxValue
      rangeTime.range must_== 2
    }
    "double again for the third one" in {
      drum.learn(2, Int.MaxValue, 1500)
      val rangeTime = drum.estimate(4000)
      rangeTime.estimateMS must_== Int.MaxValue
      rangeTime.range must_== 4
    }
    "start the drum after learned three observations" in {
      drum.learn(4, Int.MaxValue, 1800)
      val rangeTime = drum.estimate(4200)
      rangeTime.estimateMS must_== 3492
      rangeTime.range must_== 8
    }
    "update the model after learned the fourth observation" in {
      drum.learn(8, 3492, 3000)
      val rangeTime = drum.estimate(3200)
      rangeTime.estimateMS must_== 2420
      rangeTime.range must_== 6
    }
  }
}
