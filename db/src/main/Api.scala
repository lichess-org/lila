package lila.db
package api

import Implicits._

import play.api.libs.json._
import Json.JsValueWrapper

import play.modules.reactivemongo.Implicits.{ JsObjectWriter ⇒ _, _ }
import PlayReactiveMongoPatch._
import play.api.libs.concurrent.Execution.Implicits._

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands._

import org.joda.time.DateTime

object Free extends Free
trait Free extends operator {
  object select extends operator with select
  object sort extends sort
}

object Full extends Full

trait Full extends Free {
  object query extends query
  object count extends count
  object exists extends exists
  object primitive extends primitive
  object projection extends projection
}

trait operator {

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

  def $in[A: Writes](values: A*) = Json.obj("$in" -> Json.arr(values))
  def $exists(bool: Boolean) = Json.obj("$exists" -> bool)

  def $reg(value: String, flags: String = "") = BSONRegex(value, flags)
  def $date(value: DateTime) = Json.obj("$date" -> value.getMillis)

  private def wrap[K, V: Writes](pairs: Seq[(K, V)]): Seq[(K, JsValueWrapper)] = pairs map {
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

  def asc: SortOrder = SortOrder.Ascending
  def desc: SortOrder = SortOrder.Descending

  def asc(field: String): (String, SortOrder) = field -> asc
  def desc(field: String): (String, SortOrder) = field -> desc

  val ascId = asc("_id")
  val descId = desc("_id")
}

object query extends query
trait query {

  def all(implicit coll: ReactiveColl) = builder

  def apply(q: JsObject)(implicit coll: ReactiveColl) = builder query q

  def byId[A: Writes](id: A)(implicit coll: ReactiveColl) = apply(select byId id)

  def byIds[A: Writes](ids: Seq[A])(implicit coll: ReactiveColl) = apply(select byIds ids)

  def builder(implicit coll: ReactiveColl) = coll.genericQueryBuilder
}

trait find[ID: Writes, Doc <: Identified[ID]] {

  def one(q: JsObject, modifier: QueryBuilder ⇒ QueryBuilder = identity): Fu[Option[Doc]] =
    modifier(query(q)).one[Option[Doc]] map (_.flatten)

  def byId(id: ID): Fu[Option[Doc]] = one(select byId id)

  def byIds(ids: Seq[ID]): Fu[List[Doc]] = apply(select byIds ids)

  def byOrderedIds(ids: Seq[ID]): Fu[List[Doc]] = byIds(ids) map { docs ⇒
    val docsMap = docs.map(u ⇒ u.id -> u).toMap
    ids.map(docsMap.get).flatten.toList
  }

  def all: Fu[List[Doc]] = apply(select.all)

  def apply(q: JsObject): Fu[List[Doc]] = cursor(q).toList map (_.flatten)
  def apply(q: JsObject, nb: Int): Fu[List[Doc]] = cursor(q, nb) toList nb map (_.flatten)

  def apply(b: QueryBuilder): Fu[List[Doc]] = cursor(b).toList map (_.flatten)
  def apply(b: QueryBuilder, nb: Int): Fu[List[Doc]] = cursor(b, nb) toList nb map (_.flatten)
}

object count extends count
trait count {

  def apply(q: JsObject)(implicit coll: ReactiveColl): Fu[Int] =
    coll.db command Count(coll.name, JsObjectWriter.write(q).some)

  def apply(implicit coll: ReactiveColl): Fu[Int] =
    coll.db command Count(coll.name, none)
}

object exists extends exists
trait exists { 

  private object count extends count

  def apply(q: JsObject)(implicit coll: ReactiveColl): Fu[Boolean] =
    count(q) map (0 !=)

  def byId[A : Writes](id: A)(implicit coll: ReactiveColl): Fu[Boolean] =
    apply(select(id))
}

object primitive extends primitive
trait primitive {

  def apply[A](q: JsObject, field: String, modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsValue ⇒ Option[A])(implicit coll: ReactiveColl): Fu[List[A]] =
    modifier(coll.genericQueryBuilder query q projection Json.obj(field -> true)).cursor.toList map (list ⇒ list map { obj ⇒
      extract(JsObjectReader.read(obj) \ field)
    } flatten)

  def one[A](q: JsObject, field: String, modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsValue ⇒ Option[A])(implicit coll: ReactiveColl): Fu[Option[A]] =
    modifier(coll.genericQueryBuilder query q projection Json.obj(field -> true)).one map (opt ⇒ opt map { obj ⇒
      extract(JsObjectReader.read(obj) \ field)
    } flatten)
}

object projection extends projection
trait projection {

  def apply[A](q: JsObject, fields: Seq[String], modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsObject ⇒ Option[A])(implicit coll: ReactiveColl): Fu[List[A]] =
    modifier(coll.genericQueryBuilder query q projection projector(fields)).cursor.toList map (list ⇒ list map { obj ⇒
      extract(JsObjectReader read obj)
    } flatten)

  def one[A](q: JsObject, fields: Seq[String], modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsObject ⇒ Option[A])(implicit coll: ReactiveColl): Fu[Option[A]] =
    modifier(coll.genericQueryBuilder query q projection projector(fields)).one map (opt ⇒ opt map { obj ⇒
      extract(JsObjectReader read obj)
    } flatten)

  private def projector(fields: Seq[String]): JsObject = Json obj {
    (fields map (_ -> Json.toJsFieldJsValueWrapper(1))): _*
  }
}
