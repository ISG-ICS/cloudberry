package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._

object TwitterDataStoreForSQL {
  val DatasetName = "twitter_ds_tweet"
  val TimeFieldName = "create_at"
  val TwitterSchemaForSQL = Schema("twitter_ds_tweet",
                                      Seq(
                                        TimeField("create_at"),
                                        NumberField("id"),
                                        TextField("lang"),
                                        NumberField("is_retweet"),
                                        JsonField("hashtags", true),
                                        JsonField("user_mentions", true),
                                        JsonField("geo_tag", false)
                                      ),
                                      Seq(
                                        TextField("text"),
                                        JsonField("user", false),
                                        JsonField("place", false),
                                        NumberField("in_reply_to_status"),
                                        NumberField("in_reply_to_user"),
                                        NumberField("favorite_count"),
                                        NumberField("retweet_count")
                                      ),
                                      Seq(NumberField("id")),
                                      TimeField(TimeFieldName))
}