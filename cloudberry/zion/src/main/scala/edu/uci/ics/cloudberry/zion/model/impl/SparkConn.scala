package edu.uci.ics.cloudberry.zion.model.impl
import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}
import org.apache.spark.sql.Encoders
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
/**
  * Created by sicongliu on 17/5/19.
  */

class SparkConn(url: String)(implicit ec: ExecutionContext) extends IDataConn{
  val spark = SparkSession.builder
    .master(url)
    .appName("Cloudberry")
    .getOrCreate()
  // This is the schema of dataset
  val schema = (new StructType).add("create_at", TimestampType).add("id",LongType).add("favorite_count", DoubleType).add("retweet_count ", DoubleType).add("in_reply_to_status", DoubleType).add("in_reply_to_user", DoubleType).add("text", StringType).add("lang", StringType).add("is_retweet", BooleanType).add("user_mentions",ArrayType(DoubleType)).add("hashtags",ArrayType(StringType)).add("geo_tag",(new StructType).add("stateID",IntegerType).add("countyID",IntegerType).add("cityID",IntegerType)).add("user",(new StructType).add("id",IntegerType).add("statues_count",DoubleType))
  // change it to your path
  val testDF = spark.read.schema(schema).json("twitter_dataset.json")
  testDF.createOrReplaceTempView("twitter_ds_tweet")
  // just to make it work and remove this in future versions
  val berry = spark.read.json("berry.json")
  berry.createOrReplaceTempView("berrymeta")

  val defaultQueryResponse = Json.toJson(Seq(Seq.empty[JsValue]))
  def post(query: String): Future[WSResponse] = {
    throw new UnsupportedOperationException
  }
  def postQuery(query: String): Future[JsValue] = {
    val result = spark.sql(query).toJSON.collect()
    var jArr = new JsArray()
    for (rr <- result) {
      jArr = jArr :+ Json.parse(rr)
    }
    Future(jArr)
  }

  def postControl(query: String) = {
    Future(true)
  }

}
