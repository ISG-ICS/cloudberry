package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._

object TwitterDataStoreForSQL {
  val DatasetName = "twitter_ds_tweet"
  val TimeFieldName = "create_at"
  val TwitterSchemaForSQL = Schema("twitter_ds_tweet",
                                      Seq(
                                        TimeField("create_at"),
                                        TextField("hashtags"),
                                        TextField("user_mentions"),
                                        TextField("lang"),
                                        NumberField("id"),
                                        NumberField("is_retweet"),
                                        NumberField("geo_tag.stateID"),
                                        NumberField("geo_tag.countyID"),
                                        NumberField("geo_tag.cityID"),
                                        StringField("geo_tag.stateName"),
                                        StringField("geo_tag.countyName"),
                                        StringField("geo_tag.cityName")
                                      ),
                                      Seq(
                                        TextField("text"),
                                        TimeField("user.create_at"),
                                        StringField("user.description"),
                                        StringField("user.name"),
                                        StringField("user.screen_name"),
                                        StringField("user.lang"),
                                        StringField("user.location"),
                                        StringField("user.statues_count"),
                                        StringField("place.country"),
                                        StringField("place.country_code"),
                                        StringField("place.full_name"),
                                        StringField("place.id"),
                                        StringField("place.name"),
                                        StringField("place.type"),
                                        NumberField("user.id"),
                                        NumberField("user.friends_count"),
                                        NumberField("in_reply_to_status"),
                                        NumberField("in_reply_to_user"),
                                        NumberField("favorite_count"),
                                        NumberField("retweet_count"),
                                        PointField("place.bounding_box")
                                      ),
                                      Seq(NumberField("id")),
                                      TimeField(TimeFieldName))
}