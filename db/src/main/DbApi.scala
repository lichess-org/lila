package lila.db

import play.api.libs.json._
import Json.JsValueWrapper

import reactivemongo.api._
import reactivemongo.bson._

import play.modules.reactivemongo.Implicits._
import org.joda.time.DateTime

object DbApi extends DbApi

trait DbApi extends operator {
  object select extends operator with select
  object sort extends sort
}

trait operator {

  def $set[A : Writes](pairs: (String, A)*) = Json.obj("$set" -> Json.obj(wrap(pairs): _*))
  def $set(pairs: (String, JsValueWrapper)*) = Json.obj("$set" -> Json.obj(pairs: _*))
  def $unset(fields: String*) = Json.obj("$unset" -> Json.arr(fields))
  def $inc[A : Writes](pairs: (String, A)*) = Json.obj("$inc" -> Json.obj(wrap(pairs): _*))
  def $push[A : Writes](field: String, value: A) = Json.obj("$push" -> Json.obj(field -> value))
  def $pull[A : Writes](field: String, value: A) = Json.obj("$pull" -> Json.obj(field -> value))

  def $gt[A: Writes](value: A) = Json.obj("$gt" -> value)
  def $gte[A: Writes](value: A) = Json.obj("$gte" -> value)
  def $lt[A: Writes](value: A) = Json.obj("$lt" -> value)
  def $lte[A: Writes](value: A) = Json.obj("$lte" -> value)
  def $ne[A: Writes](value: A) = Json.obj("$ne" -> value)

  def $in[A: Writes](values: A*) = Json.obj("$in" -> Json.arr(values))
  def $exists(bool: Boolean) = Json.obj("$exists" -> bool)

  def $reg(value: String, flags: String = "") = BSONRegex(value, flags)
  def $date(value: DateTime) = Json.obj("$date" -> value.getMillis)

  private def wrap[K, V : Writes](pairs: Seq[(K, V)]): Seq[(K, JsValueWrapper)] = pairs map {
    case (k, v) ⇒ k -> Json.toJsFieldJsValueWrapper(v)
  }
}

object select extends operator with select
trait select { self: operator ⇒

  def all = Json.obj()

  def apply[A: Writes](id: A): JsObject = byId(id)

  def byId[A: Writes](id: A) = Json.obj("_id" -> id)

  def byIds[A: Writes](ids: Seq[A]) = Json.obj("_id" -> $in(ids))
}

object sort extends sort
trait sort {

  def naturalDesc = "$natural" -> desc

  def asc = SortOrder.Ascending
  def desc = SortOrder.Descending

  def asc(field: String) = field -> SortOrder.Ascending
  def desc(field: String) = field -> SortOrder.Descending

  val ascId = asc("_id")
  val descId = desc("_id")
}
