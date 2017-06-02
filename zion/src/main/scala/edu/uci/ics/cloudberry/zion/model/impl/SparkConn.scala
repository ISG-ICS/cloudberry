package edu.uci.ics.cloudberry.zion.model.impl
import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.libs.ws.{WSClient, WSResponse}
import edu.uci.ics.cloudberry.util.Logging
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
/**
  * Created by sicongliu on 17/5/19.
  */
/*
class SparkConn(url: String, wSClient: WSClient)(implicit ec: ExecutionContext) extends IDataConn with Logging {
  val spark = SparkSession.builder
    .master("local")
    .appName("Cloudberry")
    .getOrCreate()

  val schema = (new StructType).add("create_at", TimestampType).add("id",LongType).add("favorite_count", DoubleType).add("retweet_count ", DoubleType).add("in_reply_to_status", DoubleType).add("in_reply_to_user", DoubleType).add("text", StringType).add("lang", StringType).add("is_retweet", BooleanType).add("user_mentions",ArrayType(DoubleType)).add("hashtags",ArrayType(StringType)).add("geo_tag",(new StructType).add("stateID",IntegerType).add("countyID",IntegerType).add("cityID",IntegerType)).add("user",(new StructType).add("id",IntegerType).add("statues_count",DoubleType))
  val testDF = spark.read.schema(schema).json("big.json")
  testDF.createOrReplaceTempView("ds_tweet")
// register population
  // register city
  def post(query: String): Future[JsValue] = {
    log.debug("Query:" + query)
   // val f = wSClient.url(url).withRequestTimeout(Duration.Inf).post(params(query))
    //f.onFailure(wsFailureHandler(query))
    //f
    val result = spark.sql(query).toJSON

  }
//  def postQuery(query: String): Future[JsValue] = {
//    postWithCheckingStatus(query, (ws: WSResponse) => {
//      ws.json.asInstanceOf[JsObject].value("results")
//    }, (ws: WSResponse) => defaultQueryResponse)
//  }
//
//  def postControl(query: String): Future[Boolean] = {
//    postWithCheckingStatus(query, (ws: WSResponse) => true, (ws: WSResponse) => false)
//  }

//  protected def postWithCheckingStatus[T](query: String, succeedHandler: WSResponse => T, failureHandler: WSResponse => T): Future[T] = {
//    post(query).map { wsResponse =>
//      if (wsResponse.json.asInstanceOf[JsObject].value.get("status") == Some(JsString("success"))) {
//        succeedHandler(wsResponse)
//      }
//      else {
//        log.error("Query failed:" + Json.prettyPrint(wsResponse.json))
//        failureHandler(wsResponse)
//      }
//    }
//  }
}
*/