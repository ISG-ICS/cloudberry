package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore._
import edu.uci.ics.cloudberry.zion.model.schema._

import scala.concurrent.{ExecutionContext, Future}

class DataStore(override val schema: Schema,
                conn: IDataConn,
                responseHandler: IWSResponseHandler
               )(implicit ec: ExecutionContext)
  extends IDataStore {

  override def query(query: String): Future[IResponse] = {
    conn.query(query).map(responseHandler.handler)
  }
}

object TwitterDataStore {
  val Name = "twitter.ds_tweet"
  val TwitterSchema: Schema = new Schema(Name,
                                         Seq(
                                           TimeField("create_at"),
                                           NumberField("id"),
                                           PointField("coordinate"),
                                           StringField("lang"),
                                           BooleanField("is_retweet"),
                                           BagField("hashtags", DataType.String),
                                           BagField("user_mentions", DataType.Number),
                                           NumberField("user.id"),
                                           NumberField("geo_tag.stateID"),
                                           NumberField("geo_tag.countyID"),
                                           NumberField("geo_tag.cityID"),
                                           HierarchyField("geo", DataType.Number,
                                                          Map(
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
                                         )
  )
}
