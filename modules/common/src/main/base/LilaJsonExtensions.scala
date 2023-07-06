package lila.base

import play.api.libs.json.*
import ornicar.scalalib.newtypes.SameRuntime

trait JsonExtensions:

  extension (js: JsObject)

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
      arr(key) map { j =>
        j.value.iterator.map(as).to(List).flatten
      }

    def ints(key: String): Option[List[Int]] = arrAs(key)(_.asOpt[Int])

    def strs(key: String): Option[List[String]] = arrAs(key)(_.asOpt[String])

    def objs(key: String): Option[List[JsObject]] = arrAs(key)(_.asOpt[JsObject])

    def get[A: Reads](key: String): Option[A] =
      (js \ key).asOpt[A]

    def noNull = JsObject:
      js.fields collect {
        case (key, value) if value != JsNull => key -> value
      }

    def add(pair: (String, Boolean)): JsObject =
      if pair._2 then js + (pair._1 -> JsBoolean(true))
      else js

    def add[A](pair: (String, A))(using sr: SameRuntime[A, Boolean]): JsObject = add(pair._1, sr(pair._2))

    def add(key: String, value: Boolean): JsObject =
      if value then js + (key -> JsBoolean(true))
      else js

    def add[A](key: String, value: A)(using sr: SameRuntime[A, Boolean]): JsObject = add(key, sr(value))

    def add[A: Writes](pair: (String, Option[A])): JsObject =
      pair._2.fold(js): a =>
        js + (pair._1 -> Json.toJson(a))

    def add[A: Writes](key: String, value: Option[A]): JsObject =
      value.fold(js): a =>
        js + (key -> Json.toJson(a))

  extension (js: JsValue)

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

    def get[A: Reads](key: String): Option[A] =
      (js \ key).asOpt[A]

    def arr(key: String): Option[JsArray] =
      (js \ key).asOpt[JsArray]
