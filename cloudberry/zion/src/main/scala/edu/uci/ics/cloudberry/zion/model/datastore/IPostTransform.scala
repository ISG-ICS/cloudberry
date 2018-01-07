package edu.uci.ics.cloudberry.zion.model.datastore

import play.api.libs.json.JsValue


trait IPostTransform {
  def category: String
  def transform(jsValue: JsValue): JsValue
}

case object NoTransform extends IPostTransform {
  override def category: String = "default"
  override def transform(jsValue: JsValue): JsValue = jsValue
}

