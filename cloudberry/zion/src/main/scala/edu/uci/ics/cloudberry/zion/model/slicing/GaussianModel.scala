package edu.uci.ics.cloudberry.zion.model.slicing

import org.apache.commons.math.special.Erf

class GaussianModel{

}

object GaussianModel {
  def getOptimalRx(range: Double, W: Double, stdDev: Double, alpha: Double, a0: Double, a1: Double): Double = {
    val R = range
    val Rw = (W - a0) / a1
    val optimalValueZ = 2 * W / (a1 * R * alpha) - 1
    if (optimalValueZ < -1) {
      0
    } else if (optimalValueZ > 0) {
      Rw
    } else {
      val z = Erf.erfInv(optimalValueZ)
      val g = Math.sqrt(2) * stdDev * z + W
      val rx = (g- a0) / a1
      Math.min(Rw, Math.max(0, (Math.sqrt(2) * stdDev * z + W - a0) / a1))
    }
  }

}
