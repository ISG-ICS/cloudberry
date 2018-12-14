package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import edu.uci.ics.cloudberry.zion.model.schema.TimeField
import play.api.libs.ws.WSResponse
import play.api.libs.json.{Json, _}
import play.Logger

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.Breaks.{break, breakable}
import java.sql.{Connection, DriverManager, _}
import oracle.sql._
import oracle.spatial.geometry.JGeometry


class OracleConn(url: String)(implicit ec: ExecutionContext) extends IDataConn {
  val defaultQueryResponse = Json.toJson(Seq(Seq.empty[JsValue]))
  val connection: Connection = DriverManager.getConnection(url)



  def post(query: String): Future[WSResponse] = {
    throw new UnsupportedOperationException
  }

  def postQuery(query: String): Future[JsValue] =
    query match {
      case berry if query.contains(OracleConn.metaName)  => postBerryQuery(query)
      case _ => postGeneralQuery(query)
    }



  protected def postGeneralQuery(query: String): Future[JsValue] = {
    val statement = connection.createStatement
    val result = statement.executeQuery(query)
    val resultMetadata = result.getMetaData
    val columnCount = resultMetadata.getColumnCount
    var qJsonArray: JsArray = Json.arr()
    while (result.next) {
      val columnId = 0
      var rsJson: JsObject = Json.obj()
      breakable {
        for (columnId <- 1 to columnCount) {
          val columnLabel = resultMetadata.getColumnLabel(columnId)
          val valueType = resultMetadata.getColumnTypeName(columnId)
          val value = result.getObject(columnLabel)
          valueType match {
            case "NUMBER" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(value.toString.toDouble))
            case "DATE" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(TimeField.TimeFormat.print(value.asInstanceOf[DATE].dateValue().getTime)))
            case "TIMESTAMP" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(TimeField.TimeFormat.print(value.asInstanceOf[TIMESTAMP].dateValue().getTime)))
            case "BLOB" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
            case "LONG" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsNumber(value.toString.toLong))
            case "NCHAR" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
            case "CHAR" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
            case "VARCHAR2" =>
              if (value != null) {
                if (value.toString.contains("LINESTRING(")) {
                  val bound_boxStr = value.toString
                  val bound_boxFirstx = bound_boxStr.slice(11, bound_boxStr.length - 2).split(",")(0).split(" ")(0).toDouble
                  val bound_boxFirsty = bound_boxStr.slice(11, bound_boxStr.length - 2).split(",")(0).split(" ")(1).toDouble
                  val firstBound_box = Seq(bound_boxFirstx, bound_boxFirsty)
                  val bound_boxSecondx = bound_boxStr.slice(11, bound_boxStr.length - 2).split(",")(1).split(" ")(0).toDouble
                  val bound_boxSecondy = bound_boxStr.slice(11, bound_boxStr.length - 2).split(",")(1).split(" ")(1).toDouble
                  val secondBound_box = Seq(bound_boxSecondx, bound_boxSecondy)
                  val bound_box = Seq(firstBound_box, secondBound_box)
                  rsJson = rsJson ++ Json.obj(columnLabel -> bound_box)
                }
                else {
                  rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
                }
              }
            case "VARCHAR" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
            case "NVARCHAR2" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
            case "NCLOB" => //large data
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
            case "CLOB" =>
              rsJson = rsJson ++ Json.obj(columnLabel -> JsString(value.toString))
            case "MDSYS.SDO_GEOMETRY"=>
                if (value != null){
                    val j_geom = JGeometry.load(value.asInstanceOf[STRUCT])
                    val test = j_geom.getPoint
                    val coordinates = Seq(test(0), test(1))
                    rsJson = rsJson ++ Json.obj(columnLabel -> coordinates)
              }
            case _ =>
              Logger.warn(s"type of value $value is not detectd")
              break
          }
        }
        qJsonArray = qJsonArray :+ rsJson
      }
    }
    Future(Json.toJson(qJsonArray))
  }

  protected def postBerryQuery(query: String): Future[JsValue] = {
    val statement = connection.createStatement
    val result = statement.executeQuery(query)
    var qJsonArray: JsArray = Json.arr()
    while (result.next) {
      var rsJson: JsObject = Json.obj()
      val name = result.getObject("name")
      val schema = result.getObject("schema")
      val stats = result.getObject("stats")
      val dataInterval = result.getObject("dataInterval")
      rsJson = rsJson ++ Json.obj("name" -> JsString(name.asInstanceOf[String]))
      rsJson = rsJson ++ Json.obj("schema" -> Json.parse(schema.toString))
      rsJson = rsJson ++ Json.obj("stats" -> Json.parse(stats.toString))
      rsJson = rsJson ++ Json.obj("dataInterval" -> Json.parse(dataInterval.toString))
      qJsonArray = qJsonArray :+ rsJson
    }
    Future(Json.toJson(qJsonArray))
  }

  def postControl(query: String) = {
    val statement = connection.createStatement
    query.split("/\n").foreach {
      case q =>
        statement.executeUpdate(q)
    }
    Future(true)
  }


}

object OracleConn {
  val metaName = "berry.meta"
}
