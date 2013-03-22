package lila.common

import play.api.libs.json._

object PimpedJson extends PimpedJson

trait PimpedJson {

  implicit final class LilaPimpedJsObject(js: JsObject) {

    def str(key: String): Option[String] =
      js.value get key flatMap (_.asOpt[String])

    def int(key: String): Option[Int] =
      js.value get key flatMap (_.asOpt[Int])

    def long(key: String): Option[Long] =
      js.value get key flatMap (_.asOpt[Long])

    def obj(key: String): Option[JsObject] =
      js.value get key flatMap (_.asOpt[JsObject])

    def get[T: Reads](field: String): Option[T] = (js \ field) match {
      case JsUndefined(_) ⇒ none
      case value          ⇒ value.asOpt[T]
    }
  }

  implicit final class LilaPimpedJsValue(js: JsValue) {

    def str(key: String): Option[String] = for {
      obj ← js.asOpt[JsObject]
      value ← obj.value get key
      str ← value.asOpt[String]
    } yield str

    def int(key: String): Option[Int] = for {
      obj ← js.asOpt[JsObject]
      value ← obj.value get key
      int ← value.asOpt[Int]
    } yield int

    def long(key: String): Option[Long] = for {
      obj ← js.asOpt[JsObject]
      value ← obj.value get key
      int ← value.asOpt[Long]
    } yield int

    def obj(key: String): Option[JsObject] = for {
      obj ← js.asOpt[JsObject]
      value ← obj.value get key
      obj2 ← value.asOpt[JsObject]
    } yield obj2
  }
}
