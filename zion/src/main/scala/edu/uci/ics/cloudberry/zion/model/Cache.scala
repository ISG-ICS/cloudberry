package edu.uci.ics.cloudberry.zion.model
import edu.uci.ics.cloudberry.zion.TInterval
import edu.uci.ics.cloudberry.zion.model.Cache.{CacheResult, Entry}
import edu.uci.ics.cloudberry.zion.model.schema.{Query, Relation, Schema, TimeField}
import org.joda.time.DateTime
import play.api.libs.json.JsValue

import scala.collection.mutable.ArrayBuffer

/**
  * Created by vigne on 6/17/2017.
  */
class Cache {
  def isCovered(CacheStartTime: DateTime, CacheEndTime: DateTime, QueryStartTime: DateTime, QueryEndTime: DateTime): Boolean = {
    if (CacheStartTime == QueryStartTime && CacheEndTime == QueryEndTime) {
      true
    }

    else if (CacheStartTime == QueryStartTime){
      if ((CacheEndTime.getYear < QueryEndTime.getYear)
        || (CacheEndTime.getYear == QueryEndTime.getYear && CacheEndTime.getMonthOfYear < QueryEndTime.getMonthOfYear)
        || (CacheEndTime.getYear == QueryEndTime.getYear && CacheEndTime.getMonthOfYear == QueryEndTime.getMonthOfYear
        && CacheEndTime.getDayOfMonth < QueryEndTime.getDayOfMonth)){

        true
      }
      else{
        false
      }
    }
    else if (CacheEndTime == QueryEndTime) {
      if ((CacheStartTime.getYear > QueryStartTime.getYear)
        || (CacheStartTime.getYear == QueryStartTime.getYear && CacheStartTime.getMonthOfYear > QueryStartTime.getMonthOfYear)
        || (CacheStartTime.getYear == QueryStartTime.getYear && CacheStartTime.getMonthOfYear == QueryStartTime.getMonthOfYear
        && CacheStartTime.getDayOfMonth > QueryStartTime.getDayOfMonth)) {

        true
      }
      else {
        false
      }
    }
    else{
      false
    }

  }


  private val storage : ArrayBuffer[Entry] = new ArrayBuffer[Entry]()
  def insert(query:Query, result: JsValue) : Boolean = {
    if(!storage.contains(query)) {
      val res = Entry(query,result)
      storage += res
      true

    }
    else{
      false
    }
  }

  def get(query: Query): JsValue = {
    var QueryStartTime:DateTime = DateTime.now()
    var QueryEndTime: DateTime = DateTime.now()
    var CacheValue:JsValue = null
    if(!storage.isEmpty){
      println("Storage is not empty")
      for(filter1 <- query.filter){
        println(s"Inside Query filter:: $filter1")
        if(filter1.toString().contains("TimeField")){
          QueryStartTime= TimeField.TimeFormat.parseDateTime(filter1.values(0).asInstanceOf[String])
          QueryEndTime= TimeField.TimeFormat.parseDateTime(filter1.values(1).asInstanceOf[String])
        }
      }

      for(entry:Entry <- storage){
        val CacheQuery:Query = entry.query
        for (cacheFilter <- CacheQuery.filter) {
          if(cacheFilter.toString().contains("TimeField")){
            val CacheStartTime :DateTime = TimeField.TimeFormat.parseDateTime(cacheFilter.values(0).asInstanceOf[String])
            val CacheEndTime :DateTime = TimeField.TimeFormat.parseDateTime(cacheFilter.values(1).asInstanceOf[String])
            if (isCovered(CacheStartTime,CacheEndTime,QueryStartTime,QueryEndTime)) {
              CacheValue = entry.value
              CacheValue
            }
            else{
              CacheValue
            }
          }
          else{
            CacheValue
          }
        }
      }
      CacheValue

    }
    else{
      CacheValue
    }

  }


}

object Cache {
  case class CacheResult(result: JsValue)
  case class Entry(query: Query, value: JsValue)
}
