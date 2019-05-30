package edu.uci.ics.cloudberry.zion.model.impl

import edu.uci.ics.cloudberry.zion.model.datastore.IDataConn
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

class ElasticsearchConn(url: String, wSClient: WSClient)(implicit ec: ExecutionContext) extends IDataConn {

    import ElasticsearchConn._

    override def defaultQueryResponse: JsValue = defaultEmptyResponse

    def postQuery(query: String): Future[JsValue] = {
        postWithCheckingStatus(query, (ws: WSResponse, query) => {parseResponse(ws.json, query)}, (ws: WSResponse) => defaultQueryResponse)
    }

    def postControl(query: String): Future[Boolean] = {
        if (query.startsWith("[")) { // For view table transaction
            transactionWithCheckingStatus(query)
        } else {
            postWithCheckingStatus(query, (ws: WSResponse, query) => true, (ws: WSResponse) => false)
        }
    }

    private def postWithCheckingStatus[T](query: String, succeedHandler: (WSResponse, String) => T, failureHandler: WSResponse => T): Future[T] = {
        post(query).map { wsResponse =>
            if (wsResponse.status == SUCCESSFUl || (wsResponse.json \ "error" \ "type").get == JsString(INDEX_EXIST_ERROR_TYPE)) {
                succeedHandler(wsResponse, query)
            }
            else {
                Logger.error("Query failed: " + Json.prettyPrint(wsResponse.json))
                failureHandler(wsResponse)
            }
        }
    }

    private def transactionWithCheckingStatus(query: String): Future[Boolean] = {
        var jsonQuery = Json.parse(query).as[Seq[JsObject]]
        while (jsonQuery.length != 1) {
            val headQuery = jsonQuery.head.toString()
            jsonQuery = jsonQuery.drop(1)
            post(headQuery)
        }
        post(jsonQuery.head.toString()).map { wsResponse =>
            if (wsResponse.status == SUCCESSFUl) {
                true
            } else{
                Logger.error("Transaction query failed: " + Json.prettyPrint(wsResponse.json))
                false
            }
        }
    }

    def post(query: String): Future[WSResponse] = {
        var jsonQuery = Json.parse(query).as[JsObject]
        val method = (jsonQuery \ "method").get.toString().stripPrefix("\"").stripSuffix("\"")
        val jsonAggregation = (jsonQuery \ "aggregation" \ "func").getOrElse(JsNull)
        val aggregation = if (jsonAggregation != JsNull) jsonAggregation.toString().stripPrefix("\"").stripSuffix("\"") else ""
        var dataset = ""
        var queryURL = ""

        method match {
            case "reindex" =>  queryURL = url + "/_reindex?refresh"
            case "msearch" => queryURL = url + "/_msearch"
            case _ => {
                dataset = (jsonQuery \ "dataset").get.toString().stripPrefix("\"").stripSuffix("\"")
                queryURL = url + "/" + dataset
            }
        }
        val filterPath =
            aggregation match {
                case "" => {
                    if (method.equals("search"))
                        if ((jsonQuery \ "groupAsList").getOrElse(JsNull) == JsNull) "?filter_path=hits.hits._source" else "?filter_path=aggregations"
                    else
                        ""
                }
                case "count" => "?filter_path=hits.total"
                case "min" | "max" => {
                    val asField = (jsonQuery \ "aggregation" \ "as").get.toString().stripPrefix("\"").stripSuffix("\"")
                    s"""?size=0&filter_path=aggregations.$asField.value_as_string"""
                }
                case _ => ???
            }
        jsonQuery -= "method"
        jsonQuery -= "dataset"
        jsonQuery -= "aggregation"
        jsonQuery -= "groupAsList"
        jsonQuery -= "selectFields"
        jsonQuery -= "joinTermsFilter"

        val f = method match {
            case "search" => wSClient.url(queryURL + "/_search" + filterPath).withHeaders(("Content-Type", "application/json")).withRequestTimeout(Duration.Inf).post(jsonQuery)
            case "msearch" => {
                val queries = (jsonQuery \ "queries").get.as[List[JsValue]].mkString("", "\n", "\n") // Queries must be terminated by a new line character
                wSClient.url(queryURL).withHeaders(("Content-Type", "application/json")).withRequestTimeout(Duration.Inf).post(queries)
            }
            case "reindex" => wSClient.url(queryURL).withHeaders(("Content-Type", "application/json")).withRequestTimeout(Duration.Inf).post(jsonQuery)
            case "create" => wSClient.url(queryURL).withHeaders(("Content-Type", "application/json")).withRequestTimeout(Duration.Inf).put(jsonQuery)
            case "drop" => wSClient.url(queryURL).withRequestTimeout(Duration.Inf).delete()
            case "upsert" => {
                val records = (jsonQuery \ "records").get.as[List[JsValue]].mkString("", "\n", "\n") // Queries must be terminated by a new line character
                wSClient.url(queryURL + "/_doc" + "/_bulk?refresh").withHeaders(("Content-Type", "application/json")).withRequestTimeout(Duration.Inf).post(records)
            }
            case _ => ??? // No other method defined
        }
        f.onFailure(wsFailureHandler(query))
        f
    }

    private def wsFailureHandler(query: String): PartialFunction[Throwable, Unit] = {
        case e: Throwable => Logger.error("WS ERROR:" + query, e)
            throw e
    }

    private def parseResponse(response: JsValue, query: String): JsValue = {
        val jsonQuery = Json.parse(query).as[JsObject]
        val jsonAggregation = (jsonQuery \ "aggregation" \ "func").getOrElse(JsNull)
        val aggregation = if (jsonAggregation != JsNull) jsonAggregation.toString().stripPrefix("\"").stripSuffix("\"") else ""
        val jsonGroupAsList = (jsonQuery \ "groupAsList").getOrElse(JsNull)
        val joinSelectField = (jsonQuery \ "joinSelectField").getOrElse(JsNull)

        if (jsonGroupAsList != JsNull) {
            return parseGroupByQueryResponse(response, jsonQuery, jsonGroupAsList, joinSelectField)
        }
        return parseNonGroupByResponse(response, jsonQuery, aggregation)
    }

    private def parseGroupByQueryResponse(response: JsValue, jsonQuery: JsObject, jsonGroupAsList: JsValue, joinSelectField: JsValue): JsArray = {
        var resArray = Json.arr()
        val groupAsList = jsonGroupAsList.as[Seq[String]]

        if (joinSelectField != JsNull) { // JOIN query with aggregation
            val sortedJoinTermsFilter = (jsonQuery \ "joinTermsFilter").get.as[Seq[Int]].sorted
            val responseList= (response \ "responses").as[Seq[JsObject]]
            val buckets = (responseList.head \ "aggregations" \ groupAsList.head \ "buckets").get.as[Seq[JsObject]]
            val joinBucket = (responseList.last \ "hits" \ "hits").get.as[Seq[JsObject]]
            val joinSelectFieldString = joinSelectField.toString().stripPrefix("\"").stripSuffix("\"")

            for (bucket <- buckets) {
                val key = (bucket \ "key").get.as[Int]
                val count = (bucket \ "doc_count").get.as[Int]
                val keyIndex = binarySearch(sortedJoinTermsFilter, key)
                val joinValue = (joinBucket(keyIndex) \ "_source" \ joinSelectFieldString).get.as[Int]

                var tmp_json = Json.obj(groupAsList.head -> JsNumber(key))
                tmp_json += ("count" -> JsNumber(count))
                tmp_json += (joinSelectFieldString -> JsNumber(joinValue))
                resArray = resArray.append(tmp_json)
            }
        }
        else {
            val buckets: Seq[JsObject] = (response \ "aggregations" \ groupAsList.head \ "buckets").get.as[Seq[JsObject]]
            for (bucket <- buckets) {
                val keyValue = if (bucket.keys.contains("key_as_string")) (bucket \ "key_as_string").get else (bucket \ "key").get
                val jsLiquid = (bucket \ groupAsList.last \ "buckets").getOrElse(JsNull)
                if (jsLiquid != JsNull) {
                    val liquid = jsLiquid.as[Seq[JsObject]]
                    for (drop <- liquid) {
                        var tmp_json = Json.obj()
                        tmp_json += (groupAsList.head -> keyValue)
                        tmp_json += (groupAsList.last -> JsString((drop \ "key_as_string").get.as[String]))
                        tmp_json += ("count" -> JsNumber((drop \ "doc_count").get.as[Int]))
                        resArray = resArray.append(tmp_json)
                    }
                }
                else {
                    var tmp_json = Json.obj()
                    tmp_json += (groupAsList.head -> keyValue)
                    tmp_json += ("count" -> JsNumber((bucket \ "doc_count").get.as[Int]))
                    resArray = resArray.append(tmp_json)
                }
            }
        }
        resArray
    }

    private def parseNonGroupByResponse(response: JsValue, jsonQuery: JsObject, aggregation: String): JsValue = {
        aggregation match {
            case "" => { // Search query without aggregation
                val sourceJsValue = (response.asInstanceOf[JsObject] \ "hits" \ "hits").getOrElse(JsNull)
                if (sourceJsValue != JsNull) {
                    val sourceArray = sourceJsValue.as[Seq[JsObject]]
                    if (jsonQuery.keys.contains("_source")) {
                        val returnArray = sourceArray.map(doc => parseSource(doc.value("_source").as[JsObject]))
                        return Json.toJson(returnArray)
                    }
                    val returnArray = sourceArray.map(doc => doc.value("_source"))
                    return Json.toJson(returnArray)
                }
                return Json.arr()
            }
            case "count" => {
                val asField = (jsonQuery \ "aggregation" \ "as").get.toString().stripPrefix("\"").stripSuffix("\"")
                val count = (response.asInstanceOf[JsObject] \ "hits" \ "total").get.as[JsNumber]
                return Json.arr(Json.obj(asField -> count))
            }
            case "min" | "max" => {
                val asField = (jsonQuery \ "aggregation" \ "as").get.toString().stripPrefix("\"").stripSuffix("\"")

                if (response.as[JsObject].keys.nonEmpty) {
                    val min_obj = (response \ "aggregations" \ asField).as[JsObject]
                    val res = if (min_obj.keys.contains("value_as_string")) (min_obj \ "value_as_string").getOrElse(JsNull) else (min_obj \ "value").get
                    if (res != JsNull) {
                        val jsonObjRes = Json.obj(asField -> res)
                        return Json.arr(jsonObjRes)
                    }
                }
                return Json.arr()
            }
            case _ => ???
        }
    }

    private def parseSource(source: JsObject): JsValue = { // Parse "_source" field in Elasticsearch response for heat map and pin map
        var returnSource = Json.obj()
        source.keys.foreach(key => {
            var curKey = key
            var value = (source \ curKey).get
            while (value.isInstanceOf[JsObject]) {
                val tmp = value.as[JsObject]
                curKey += "." + tmp.keys.head
                value = tmp.values.head
            }
            val valueString = value.toString().stripPrefix("\"").stripSuffix("\"")
            if (valueString.startsWith("point")) {
                val arrString = "[" + valueString.stripPrefix("point(").stripSuffix(")").replace(" ", ",") + "]"
                returnSource += (curKey -> Json.parse(arrString))
            }
            else if (valueString.startsWith("LINESTRING")) {
                val arrString = "[[" + valueString.stripPrefix("LINESTRING(").stripSuffix(")").replace(",", "],[").replace(" ", ",") + "]]"
                returnSource += (curKey -> Json.parse(arrString))
            }
            else{
                returnSource += (curKey -> value)
            }
        })
        returnSource
    }

    private def binarySearch(arr: Seq[Int], x: Int): Int = { // Find the index of an element in an array using binary search in O(logN) time.
        var left = 0
        var right = arr.length - 1

        while (left <= right) {
            val middle = left + (right - left) / 2
            if (arr(middle) == x) {
                return middle
            } else if(arr(middle) < x) {
                left = middle + 1
            } else {
                right = middle - 1
            }
        }
        -1
    }
}

object ElasticsearchConn {
    val defaultEmptyResponse = Json.toJson(Seq(Seq.empty[JsValue]))
    val SUCCESSFUl = 200
    val INDEX_EXIST_ERROR_TYPE = "resource_already_exists_exception"
}