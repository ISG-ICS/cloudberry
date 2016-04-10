package models

class DataSet(val name: String, val timeField: String, val keywordField: String, val levelToEntityFieldMap: Map[Int, String]) {

}


object DataSet {
  val Twitter = new DataSet("ds_tweets", "create_at", "text_msg", Map(1 -> "state", 2 -> "county", 3 -> "city"))
}
