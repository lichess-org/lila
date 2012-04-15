package lila

import play.api.libs.json._

object RichJs {

  implicit def richJsObject(js: JsObject) = new {
    def str(key: String): Option[String] = js.value get key map (_.as[String])
    def obj(key: String): Option[JsObject] = js.value get key map (_.as[JsObject])
  }

  implicit def richJsValue(js: JsValue) = new {
    def str(key: String): Option[String] =
      js.as[JsObject].value get key map (_.as[String])
  }
}
