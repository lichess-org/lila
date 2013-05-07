package lila.common

import play.api.libs.json._

object PimpedJson extends PimpedJson

trait PimpedJson {

  implicit final class LilaPimpedJsObject(js: JsObject) {

    def str(key: String): Option[String] =
      (js \ key).asOpt[String]

    def int(key: String): Option[Int] =
      (js \ key).asOpt[Int]

    def long(key: String): Option[Long] =
      (js \ key).asOpt[Long]

    def obj(key: String): Option[JsObject] =
      (js \ key).asOpt[JsObject]

    def arr(key: String): Option[JsArray] =
      (js \ key).asOpt[JsArray]

    def get[A: Reads](key: String): Option[A] =
      (js \ key).asOpt[A]
  }

  implicit final class LilaPimpedJsValue(js: JsValue) {

    def str(key: String): Option[String] =
      js.asOpt[JsObject] flatMap { obj ⇒
        (obj \ key).asOpt[String]
      }

    def int(key: String): Option[Int] =
      js.asOpt[JsObject] flatMap { obj ⇒
        (obj \ key).asOpt[Int]
      }

    def long(key: String): Option[Long] =
      js.asOpt[JsObject] flatMap { obj ⇒
        (obj \ key).asOpt[Long]
      }

    def obj(key: String): Option[JsObject] =
      js.asOpt[JsObject] flatMap { obj ⇒
        (obj \ key).asOpt[JsObject]
      }

    def arr(key: String): Option[JsArray] =
      js.asOpt[JsObject] flatMap { obj ⇒
        (obj \ key).asOpt[JsArray]
      }
  }
}
