package edu.uci.ics.cloudberry.zion.model.impl
import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import java.sql.{Connection, DriverManager}

import org.joda.time._
import play.api.libs.json._
import play.api.libs.json.Json
import play.api.libs.json.Json._
import play.api.libs.ws.{WSClient, WSResponse}
import edu.uci.ics.cloudberry.util.Logging
import org.apache.spark.sql.Encoders

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json.Json
import java.io.InputStream

/**
  * Created by sicongliu on 17/5/19.
  */

class SQLConn(url: String)(implicit ec: ExecutionContext) extends IDataConn{
  val driver = "com.mysql.jdbc.Driver"
  val username = "root"
  val password = "2048"
  var connection:Connection = _
  Class.forName(driver)
  connection = DriverManager.getConnection(url, username, password)
  val statement = connection.createStatement
  val defaultQueryResponse = Json.toJson(Seq(Seq.empty[JsValue]))
  val stream : InputStream = getClass.getResourceAsStream("/ddl/berry.json")
  println("STREAM SOLVED")
  val source = scala.io.Source.fromInputStream(stream).getLines.mkString
  println("SOURCE SOLVED")
  val berry: JsValue = Json.parse(source)
  println("berry is loaded: "+berry.toString())
  // upsert berry schema



  def post(query: String): Future[WSResponse] = {
    println("")
    println("post")
    println(query)
    throw new UnsupportedOperationException
  }

  def postQuery(query: String): Future[JsValue] = {
    println("")
    println("SQLConn: postQuery")
    println(query)
    if (query.contains("berry.meta")) {
      println("SQL postQuery: Contains berry.meta")
      return Future(berry)
    } else {
      println("SQL postQuery: Doesn't contain berry.meta")
      val result = statement.executeQuery(query)
      println("result executed")
      while (result.next) {
        println("result")
        val host = result.getString("create_at")
        println("create_at = %s".format(host))
      }
      var jArr = new JsArray()
      jArr = jArr :+ Json.parse("{1,2,3}")
      println("jArr: " + jArr.toString())
      return Future(Json.parse("[1,2,3]"))
    }


//    while (result.next) {
////      jArr = jArr :+ Json.parse(rr)  // change it into Json, how???
//    }
  }

  def postControl(query: String) = {
    println("")
    println("postControl")
    println(query)
    Future(true)
  }

}
