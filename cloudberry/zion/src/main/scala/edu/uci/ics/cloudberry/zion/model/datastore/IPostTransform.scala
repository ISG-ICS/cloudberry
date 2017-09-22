package edu.uci.ics.cloudberry.zion.model.datastore

import play.api.libs.json.JsValue


trait IPostTransform {
  def transform(jsValue: JsValue): JsValue
}

case object NoTransform extends IPostTransform {
  override def transform(jsValue: JsValue): JsValue = jsValue
}

