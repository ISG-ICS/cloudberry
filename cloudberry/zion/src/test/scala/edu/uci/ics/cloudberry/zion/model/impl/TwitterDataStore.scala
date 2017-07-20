package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._

object TwitterDataStore {
  val DatasetName = "twitter.ds_tweet"
  val TimeFieldName = "create_at"
  val TwitterSchema = Schema("twitter.typeTweet",
                                     Seq(
                                       TimeField(TimeFieldName),
                                       NumberField("id"),
                                       PointField("coordinate"),
                                       StringField("lang"),
                                       BooleanField("is_retweet"),
                                       BagField("hashtags", DataType.String, true),
                                       BagField("user_mentions", DataType.Number, true),
                                       NumberField("user.id"),
                                       NumberField("geo_tag.stateID"),
                                       NumberField("geo_tag.countyID"),
                                       NumberField("geo_tag.cityID"),
                                       HierarchyField("geo", DataType.Number,
                                                      Seq(
                                                        "state" -> "geo_tag.stateID",
                                                        "county" -> "geo_tag.countyID",
                                                        "city" -> "geo_tag.cityID"
                                                      ))
                                     ),
                                     Seq(
                                       TextField("text"),
                                       NumberField("in_reply_to_status"),
                                       NumberField("in_reply_to_user"),
                                       NumberField("favorite_count"),
                                       NumberField("retweet_count"),
                                       NumberField("user.status_count")
                                     ),
                                     Seq(NumberField("id")),
                                     TimeField(TimeFieldName))
}
