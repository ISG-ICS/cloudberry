package edu.uci.ics.cloudberry.zion.model.impl
import edu.uci.ics.cloudberry.zion.model.schema._

object TwitterDataStoreWithoutHashTagOracle {

  val DatasetName = "twitter.ds_tweet"
  val TimeFieldName = "create_at"
  val TwitterSchemaForOracle = Schema("twitter.ds_tweet",
    Seq(
      TimeField("create_at"),
      NumberField("id"),
      PointField("coordinate",false),
      StringField("lang"),
      BooleanField("is_retweet",false),
      BagField("hashtags", DataType.String, true),
      BagField("user_mentions", DataType.Number, true),
      NumberField("user.id"),
      StringField("user.name"),
      StringField("user.profile_image_url"),
      NumberField("geo_tag.stateID", true),
      NumberField("geo_tag.countyID", true),
      NumberField("geo_tag.cityID", true)
    ),
    Seq(
      StringField("place.bounding_box", true),
      TextField("text",false),
      NumberField("in_reply_to_status",false),
      NumberField("in_reply_to_user",false),
      NumberField("favorite_count",false),
      NumberField("retweet_count",false),
      NumberField("user.status_count",false)
    ),
    Seq(NumberField("id")),
    TimeField(TimeFieldName))

}
