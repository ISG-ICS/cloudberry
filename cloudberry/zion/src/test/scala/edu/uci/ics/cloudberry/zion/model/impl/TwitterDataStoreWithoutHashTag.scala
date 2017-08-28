package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.schema._

object TwitterDataStoreWithoutHashTag {
  val DatasetName = "twitter_ds_tweet"
  val TimeFieldName = "create_at"
  val TwitterSchemaForSQL = Schema("twitter_ds_tweet",
                                      Seq(
                                        TimeField("create_at"),
                                        NumberField("id"),
                                        NumberField("is_retweet"),
                                        NumberField("geo_tag.stateID", true),
                                        NumberField("geo_tag.countyID", true),
                                        NumberField("geo_tag.cityID", true),
                                        PointField("coordinate", true),
                                        StringField("lang"),
                                        StringField("geo_tag.stateName", true),
                                        StringField("geo_tag.countyName", true),
                                        StringField("geo_tag.cityName", true)
                                      ),
                                      Seq(
                                        TextField("text"),
                                        TextField("user.description"),
                                        TimeField("user.create_at"),
                                        StringField("user.name"),
                                        StringField("user.screen_name"),
                                        StringField("user.lang"),
                                        StringField("user.location"),
                                        StringField("user.statues_count"),
                                        StringField("place.country", true),
                                        StringField("place.country_code", true),
                                        StringField("place.full_name", true),
                                        StringField("place.id", true),
                                        StringField("place.name", true),
                                        StringField("place.type", true),
                                        StringField("place.bounding_box", true),
                                        NumberField("user.id"),
                                        NumberField("user.friends_count"),
                                        NumberField("in_reply_to_status"),
                                        NumberField("in_reply_to_user"),
                                        NumberField("favorite_count"),
                                        NumberField("retweet_count")
                                      ),
                                      Seq(NumberField("id")),
                                      TimeField(TimeFieldName))
}