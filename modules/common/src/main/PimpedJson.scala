package lila.common

import play.api.libs.json._

object PimpedJson {

  implicit final class LilaPimpedJsObject(val js: JsObject) extends AnyVal {

    def str(key: String): Option[String] =
      (js \ key).asOpt[String]

    def int(key: String): Option[Int] =
      (js \ key).asOpt[Int]

    def long(key: String): Option[Long] =
      (js \ key).asOpt[Long]

    def boolean(key: String): Option[Boolean] =
      (js \ key).asOpt[Boolean]

    def obj(key: String): Option[JsObject] =
      (js \ key).asOpt[JsObject]

    def arr(key: String): Option[JsArray] =
      (js \ key).asOpt[JsArray]

    def arrAs[A](key: String)(as: JsValue => Option[A]): Option[List[A]] =
      arr(key) map { _.value.toList map as flatten }

    def ints(key: String): Option[List[Int]] = arrAs(key)(_.asOpt[Int])

    def get[A: Reads](key: String): Option[A] =
      (js \ key).asOpt[A]

    def noNull = JsObject {
      js.fields collect {
        case (key, value) if value != JsNull => key -> value
      }
    }
  }

  implicit final class LilaPimpedJsValue(val js: JsValue) extends AnyVal {

    def str(key: String): Option[String] =
      js.asOpt[JsObject] flatMap { obj =>
        (obj \ key).asOpt[String]
      }

    def int(key: String): Option[Int] =
      js.asOpt[JsObject] flatMap { obj =>
        (obj \ key).asOpt[Int]
      }

    def long(key: String): Option[Long] =
      js.asOpt[JsObject] flatMap { obj =>
        (obj \ key).asOpt[Long]
      }

    def boolean(key: String): Option[Boolean] =
      js.asOpt[JsObject] flatMap { obj =>
        (obj \ key).asOpt[Boolean]
      }

    def obj(key: String): Option[JsObject] =
      js.asOpt[JsObject] flatMap { obj =>
        (obj \ key).asOpt[JsObject]
      }

    def arr(key: String): Option[JsArray] =
      js.asOpt[JsObject] flatMap { obj =>
        (obj \ key).asOpt[JsArray]
      }
  }
}
