package edu.uci.ics.cloudberry.zion.model.impl

/**
  * Created by yamamuraisao on 17/7/20.
  */

import edu.uci.ics.cloudberry.zion.model.schema._

object TwitterDataStoreForSQL{
  val DatasetName = "twitter_ds_tweet"
  val TimeFieldName = "create_at"
  val TwitterSchemaForSQL = Schema("twitter.typeTweet",
                                    Seq(
                                      TimeField("create_at"),
                                      NumberField("id"),
                                      StringField("lang"),
                                      BooleanField("is_retweet"),
                                      JsonField("hashtags", true),
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