package lila.db
package api

import Implicits._

import play.api.libs.json._
import Json.JsValueWrapper

import play.modules.reactivemongo.Implicits._
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
  object cursor extends cursor
  object count extends count
  object find extends find
  object exists extends exists
  object primitive extends primitive
  object projection extends projection
  object insert extends insert
  object update extends update
  object remove extends remove
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

  def all(implicit coll: Coll) = builder

  def apply(q: JsObject)(implicit coll: Coll) = builder query q

  def byId[A: Writes](id: A)(implicit coll: Coll) = apply(select byId id)

  def byIds[A: Writes](ids: Seq[A])(implicit coll: Coll) = apply(select byIds ids)

  def builder(implicit coll: Coll) = coll.genericQueryBuilder
}

object cursor extends cursor
trait cursor {

  def apply[A: Tube](q: JsObject)(implicit coll: Coll): Cursor[Option[A]] =
    apply(query(q))

  def apply[A: Tube](q: JsObject, nb: Int)(implicit coll: Coll): Cursor[Option[A]] =
    apply(query(q), nb)

  def apply[A: Tube](b: QueryBuilder)(implicit coll: Coll): Cursor[Option[A]] =
    b.cursor[Option[A]]

  def apply[A: Tube](b: QueryBuilder, nb: Int)(implicit coll: Coll): Cursor[Option[A]] =
    apply(b limit nb)
}

object find extends find
trait find {

  def one[A: Tube](
    q: JsObject,
    modifier: QueryBuilder ⇒ QueryBuilder = identity)(implicit coll: Coll): Fu[Option[A]] =
    modifier(query(q)).one[Option[A]] map (_.flatten)

  def byId[ID: Writes, A: Tube](id: ID)(implicit coll: Coll): Fu[Option[A]] =
    one(select byId id)

  def byIds[ID: Writes, A: Tube](ids: Seq[ID])(implicit coll: Coll): Fu[List[A]] =
    apply(select byIds ids)

  def byOrderedIds[ID: Writes, A <: Identified[ID]: Tube](ids: Seq[ID])(implicit coll: Coll): Fu[List[A]] =
    byIds(ids) map { docs ⇒
      val docsMap = docs.map(u ⇒ u.id -> u).toMap
      ids.map(docsMap.get).flatten.toList
    }

  def all[A: Tube](implicit coll: Coll): Fu[List[A]] = apply(select.all)

  def apply[A: Tube](q: JsObject)(implicit coll: Coll): Fu[List[A]] =
    cursor(q).toList map (_.flatten)

  def apply[A: Tube](q: JsObject, nb: Int)(implicit coll: Coll): Fu[List[A]] =
    cursor(q, nb) toList nb map (_.flatten)

  def apply[A: Tube](b: QueryBuilder)(implicit coll: Coll): Fu[List[A]] =
    cursor(b).toList map (_.flatten)

  def apply[A: Tube](b: QueryBuilder, nb: Int)(implicit coll: Coll): Fu[List[A]] =
    cursor(b, nb) toList nb map (_.flatten)
}

object insert extends insert
trait insert {

  def apply[A](doc: A)(implicit coll: Coll, json: Tube[A]): Funit =
    (json toMongo doc).fold(fuck(_), apply(_))

  def apply(js: JsObject)(implicit coll: Coll): Funit =
    coll insert js flatMap { lastErr ⇒
      lastErr.ok.fold(funit, fuck(lastErr.message))
    }
}

object update extends update
trait update extends operator {

  def apply[ID : Writes, A <: Identified[ID]](doc: A)(implicit coll: Coll, tube: Tube[A]): Funit = 
    (tube toMongo doc).fold(fuck(_), js ⇒ apply(select(doc.id), js))

  def apply[A](selector: JsObject, update: JsObject, upsert: Boolean = false, multi: Boolean = false)(implicit coll: Coll): Funit = for {
    lastErr ← coll.update(selector, update, upsert = upsert, multi = multi)
    result ← lastErr.ok.fold(funit, fuck(lastErr.message))
  } yield result

  def doc[ID : Writes, A <: Identified[ID] : Tube](id: ID)(op: A ⇒ JsObject)(implicit coll: Coll): Funit =
    find byId id flatMap { docOption ⇒
      docOption zmap (doc ⇒ update(select(id), op(doc)))
    }

  def field[ID: Writes, A <: Identified[ID] : Tube](id: ID, field: String, value: A)(implicit coll: Coll): Funit =
    update(select(id), $set(field -> value))
}

object remove extends remove
trait remove {

  def apply(selector: JsObject)(implicit coll: Coll): Funit = (coll remove selector).void

  def byId[ID : Writes](id: ID)(implicit coll: Coll): Funit = apply(select(id))

  def byIds[ID : Writes](ids: Seq[ID])(implicit coll: Coll): Funit = apply(select byIds ids)
}

object count extends count
trait count {

  def apply(q: JsObject)(implicit coll: Coll): Fu[Int] =
    coll.db command Count(coll.name, JsObjectWriter.write(q).some)

  def apply(implicit coll: Coll): Fu[Int] =
    coll.db command Count(coll.name, none)
}

object exists extends exists
trait exists {

  private object count extends count

  def apply(q: JsObject)(implicit coll: Coll): Fu[Boolean] =
    count(q) map (0 !=)

  def byId[A: Writes](id: A)(implicit coll: Coll): Fu[Boolean] =
    apply(select(id))
}

object primitive extends primitive
trait primitive {

  def apply[A](q: JsObject, field: String, modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsValue ⇒ Option[A])(implicit coll: Coll): Fu[List[A]] =
    modifier(coll.genericQueryBuilder query q projection Json.obj(field -> true)).cursor.toList map (list ⇒ list map { obj ⇒
      extract(JsObjectReader.read(obj) \ field)
    } flatten)

  def one[A](q: JsObject, field: String, modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsValue ⇒ Option[A])(implicit coll: Coll): Fu[Option[A]] =
    modifier(coll.genericQueryBuilder query q projection Json.obj(field -> true)).one map (opt ⇒ opt map { obj ⇒
      extract(JsObjectReader.read(obj) \ field)
    } flatten)
}

object projection extends projection
trait projection {

  def apply[A](q: JsObject, fields: Seq[String], modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsObject ⇒ Option[A])(implicit coll: Coll): Fu[List[A]] =
    modifier(coll.genericQueryBuilder query q projection projector(fields)).cursor.toList map (list ⇒ list map { obj ⇒
      extract(JsObjectReader read obj)
    } flatten)

  def one[A](q: JsObject, fields: Seq[String], modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsObject ⇒ Option[A])(implicit coll: Coll): Fu[Option[A]] =
    modifier(coll.genericQueryBuilder query q projection projector(fields)).one map (opt ⇒ opt map { obj ⇒
      extract(JsObjectReader read obj)
    } flatten)

  private def projector(fields: Seq[String]): JsObject = Json obj {
    (fields map (_ -> Json.toJsFieldJsValueWrapper(1))): _*
  }
}
