package lila.db
package api

import play.api.libs.json._
import Json.JsValueWrapper

import reactivemongo.bson._
import play.modules.reactivemongo.json.BSONFormats

import org.joda.time.DateTime

object $operator extends $operator
trait $operator {

  def $set[A: Writes](pairs: (String, A)*) = Json.obj("$set" -> Json.obj(wrap(pairs): _*))
  def $set(pairs: (String, JsValueWrapper)*) = Json.obj("$set" -> Json.obj(pairs: _*))
  def $unset(fields: String*) = Json.obj("$unset" -> Json.arr(fields))
  def $inc[A: Writes](pairs: (String, A)*) = Json.obj("$inc" -> Json.obj(wrap(pairs): _*))
  def $push[A: Writes](field: String, value: A) = Json.obj("$push" -> Json.obj(field -> value))
  def $pull[A: Writes](field: String, value: A) = Json.obj("$pull" -> Json.obj(field -> value))

  def $gt[A: Writes](value: A) = Json.obj("$gt" -> value)
  def $gte[A: Writes](value: A) = Json.obj("$gte" -> value)
  def $lt[A: Writes](value: A) = Json.obj("$lt" -> value)
  def $lte[A: Writes](value: A) = Json.obj("$lte" -> value)
  def $ne[A: Writes](value: A) = Json.obj("$ne" -> value)

  def $in[A: Writes](values: Iterable[A]) = Json.obj("$in" -> values)
  def $nin[A: Writes](values: Iterable[A]) = Json.obj("$nin" -> values)
  def $all[A: Writes](values: Iterable[A]) = Json.obj("$all" -> values)
  def $exists(bool: Boolean) = Json.obj("$exists" -> bool)

  def $or[A: Writes](conditions: Iterable[A]) = Json.obj("$or" -> conditions)

  def $regex(value: String, flags: String = "") = BSONFormats toJSON BSONRegex(value, flags)

  def $date(value: DateTime) = BSONFormats toJSON BSONDateTime(value.getMillis)

  private def wrap[K, V: Writes](pairs: Seq[(K, V)]): Seq[(K, JsValueWrapper)] = pairs map {
    case (k, v) ⇒ k -> Json.toJsFieldJsValueWrapper(v)
  }
}
