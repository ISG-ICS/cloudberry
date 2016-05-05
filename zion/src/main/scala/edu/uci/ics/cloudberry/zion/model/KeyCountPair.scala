package edu.uci.ics.cloudberry.zion.model

import scala.collection.mutable

case class KeyCountPair(key: String, count: Int)

object KeyCountPair {
  def keyCountMerge(kc1: Seq[KeyCountPair], kc2: Seq[KeyCountPair]): Seq[KeyCountPair] = {
    val map = mutable.Map.empty[String, Int]
    map ++= kc1.map(kv => kv.key -> kv.count)
    kc2.foreach(kv => {
      val vl = map.getOrElse(kv.key, 0)
      map.update(kv.key, vl + kv.count)
    })
    map.toSeq.map(e => KeyCountPair(e._1, e._2))
  }
}