package edu.uci.ics.cloudberry.zion.model.slicing

import org.apache.commons.math3.special.Erf
import org.apache.commons.math3.fitting.{PolynomialCurveFitter, WeightedObservedPoints}

class Drum(totalRange: Int, alpha: Double, minRange: Int) {

  import Drum._

  private val stats = Seq.newBuilder[MiniQueryStats]

  def learn(range: Int, estimateMS: Int, actualMS: Int): Unit = {
    stats += MiniQueryStats(range, estimateMS, actualMS)
  }


  def estimate(limit: Int): RangeTime = {
    val history = stats.result()
    if (history.size < 1) {
      return RangeTime(minRange, Int.MaxValue)
    }

    val lastRange = history.last.range
    val lastTime = history.last.actualMS + Double.MinPositiveValue
    val linearEstimate = lastRange * limit / lastTime

    val closeRange = validateRange(linearEstimate, minRange, lastRange)
    if (history.size < 3) { // too few observations
      return RangeTime(closeRange.toInt, Int.MaxValue)
    }

    val coeff = trainLinearModel(history)
    val variance = calcVariance(history, coeff)
    val stdDev = Math.sqrt(variance)

    val rawRange = getOptimalRx(totalRange, limit, stdDev, alpha, coeff.a0, coeff.a1)
    val validRange = validateRange(rawRange, minRange, lastRange)

    val estimateTime = validRange * coeff.a1 + coeff.a0
    RangeTime(validRange.toInt, estimateTime.toInt)
  }

}

object Drum {

  def getOptimalRx(totalRange: Double, limit: Double, stdDev: Double, alpha: Double, a0: Double, a1: Double): Double = {
    val R = totalRange
    val Rw = (limit - a0) / a1
    val optimalValueZ = 2 * limit / (a1 * R * alpha) - 1
    if (optimalValueZ < -1) {
      0
    } else if (optimalValueZ > 0) {
      Rw
    } else {
      val z = Erf.erfInv(optimalValueZ)
      Math.min(Rw, Math.max(0, (Math.sqrt(2) * stdDev * z + limit - a0) / a1))
    }
  }

  def calcVariance(history: Seq[MiniQueryStats], coeff: Coeff): Double = {
    if (history.size <= 3) {
      return history.map(s => Math.pow(s.range * coeff.a1 + coeff.a0 - s.actualMS, 2)).sum / history.size
    }
    val valid = history.filterNot(h => h.estimateMS == Int.MaxValue)
    valid.map(h => (h.estimateMS - h.actualMS) * (h.estimateMS - h.actualMS)).sum.toDouble / valid.size
  }


  def trainLinearModel(history: Seq[MiniQueryStats]): Coeff = {
    val obs: WeightedObservedPoints = new WeightedObservedPoints()
    history.foreach(h => obs.add(h.range, h.actualMS))

    val rawCoeff = linearFitting(obs)

    if (rawCoeff.a0 <= Double.MinPositiveValue || rawCoeff.a1 <= Double.MinPositiveValue) {
      Coeff(Double.MinPositiveValue, history.last.actualMS.toDouble / history.last.range)
    } else {
      rawCoeff
    }
  }

  def linearFitting(obs: WeightedObservedPoints): Coeff = {
    val filter: PolynomialCurveFitter = PolynomialCurveFitter.create(1)
    val ret = filter.fit(obs.toList)
    Coeff(ret(0), ret(1))
  }

  def validateRange(range: Double, minRange: Double, lastRange: Double): Int= {
    Math.ceil(Math.max(minRange, Math.min(range.toInt, lastRange * 2))).toInt
  }

  case class MiniQueryStats(range: Int, estimateMS: Int, actualMS: Int)

  case class RangeTime(range: Int, estimateMS: Int)

  /**
    * a0 + a1 * x
    */
  case class Coeff(a0: Double, a1: Double) {
    override def toString: String = s"a1=$a1, a0=$a0"
  }

}
