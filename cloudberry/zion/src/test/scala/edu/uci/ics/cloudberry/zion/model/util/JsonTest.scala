package edu.uci.ics.cloudberry.zion.model.util

import org.specs2.mutable.Specification
import play.api.libs.json._

class JsonTest extends Specification {

  "Json object" should {
    "replace the old value by calling ++ " in {
      val jsObj = Json.obj("key" -> JsNumber(1), "v" -> JsNumber(2)) ++ Json.obj("v" -> JsNumber(42))

      (jsObj \ "key").as[Int] must_== 1
      (jsObj \ "v").as[Int] must_== 42
    }
    "merge two js array by adding the value" in {
      val jsArray1 = JsArray(1 to 1000 map (id => Json.obj("k" -> JsString(s"a$id"), "v" -> JsNumber(id))))
      val jsArray2 = JsArray(500 to 1500 map (id => Json.obj("k" -> JsString(s"a$id"), "v" -> JsNumber(id))))

      import edu.uci.ics.cloudberry.util.Profile._
      profile("using Js") {
        val updatedArray1 = {
          jsArray1.value.map { jsValue =>
            val key = jsValue \ "k"
            val value = (jsValue \ "v").as[JsNumber].value
            jsArray2.value.find(r => (r \ "k") == key) match {
              case Some(v) =>
                val newVal = JsNumber((v \ "v").as[JsNumber].value + value)
                jsValue.as[JsObject] ++ Json.obj("v" -> newVal)
              case None => jsValue
            }
          }
        }

        val onlyInArray2 = {
          jsArray2.value.filter(r2 => updatedArray1.forall(r1 => (r1 \ "k") != (r2 \ "k")))
        }
        val ret = (JsArray(updatedArray1 ++ onlyInArray2))
      }


      profile("using map") {
        val builder = scala.collection.mutable.Map[Seq[JsValue], Seq[JsValue]]()
        builder ++= jsArray1.value.map { r =>
          val key = (r \ "k").get
          val value = (r \ "v").get
          Seq(key) -> Seq(value)
        }

        for (r <- jsArray2.value) {
          val key = (r \ "k").get
          val value = (r \ "v").get
          builder.get(Seq(key)) match {
            case Some(v) =>
              val newValue = Json.toJson(v.head.as[Int] + value.as[Int])
              builder += (Seq(key) -> Seq(newValue))
            case None => builder += (Seq(key) -> Seq(value))
          }
        }
        val ret = (JsArray(builder.toMap.map { case (keys, values) =>
          val keySeq = keys.map { key =>
            "k" -> key
          }
          val valueSeq = values.map { value =>
            "v" -> value
          }
          JsObject(keySeq ++ valueSeq)
        }.toSeq))
      }
      ok
    }
  }

}
