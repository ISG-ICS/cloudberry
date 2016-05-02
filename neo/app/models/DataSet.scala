package models

class DataSet(val name: String, val timeField: String, val keywordField: String, val levelToEntityFieldMap: Map[Int, String]) {

}

class SnapShotDataSet(val name: String) {

}

object DataSet {
  import db.Migration_20160324._
  val Twitter = new DataSet(TweetDataSet, "create_at", "text", Map(1 -> "state", 2 -> "county", 3 -> "city"))
  val TwitterSnapshot = new SnapShotDataSet(SnapshotDataSet)
}

