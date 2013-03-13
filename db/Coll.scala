package lila.db

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands._
import reactivemongo.api.collections.default.BSONCollection

import play.modules.reactivemongo.Implicits._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

abstract class Coll[Doc <: WithStringId](coll: BSONCollection, json: JsonTube[Doc]) {

  object count {

    def apply(q: JsObject): Fu[Int] = db command Count(name, JsObjectWriter.write(q).some)

    def apply: Fu[Int] = db command Count(name, none)
  }

  object find {

    def one(q: JsObject): Fu[Option[Doc]] = query(q).one[Option[Doc]] map (_.flatten)

    def byId(id: ID): Fu[Option[Doc]] = one(select byId id)

    def byIds(ids: Seq[ID]): Fu[List[Doc]] = apply(select byIds ids)

    def byOrderedIds(ids: Seq[ID]): Fu[List[Doc]] = byIds(ids) map { docs ⇒
      val docsMap = docs.map(u ⇒ u.id -> u).toMap
      ids.map(docsMap.get).flatten.toList
    }

    def apply(q: JsObject): Fu[List[Doc]] = cursor(q).toList map (_.flatten)

    def apply(q: JsObject, nb: Int): Fu[List[Doc]] = cursor(q, nb) toList nb map (_.flatten)
  }

  object insert {

    def apply(doc: Doc): Funit = (json toMongo doc).fold(fuck(_), js ⇒ for {
      lastErr ← coll insert js
      result ← lastErr.ok.fold(funit, fuck(lastErr.message))
    } yield result)

    def unchecked(doc: Doc): Funit = {
      json toMongo doc foreach { coll.insert(_) }
      funit
    }
  }

  object update {

    def apply(selector: JsObject, update: JsObject, upsert: Boolean = false, multi: Boolean = false): Funit = for {
      lastErr ← coll.update(selector, update, upsert = upsert, multi = multi)
      result ← lastErr.ok.fold(funit, fuck(lastErr.message))
    } yield result

    def unchecked(selector: JsObject, update: JsObject, upsert: Boolean = false, multi: Boolean = false): Funit = {
      coll.uncheckedUpdate(selector, update, upsert = upsert, multi = multi)
      funit
    }
  }

  object projection {

    def primitive[A](q: JsObject, field: String)(extract: JsValue ⇒ Option[A]): Fu[List[A]] =
      coll.find(q, Json.obj(field -> 1)).cursor.toList map (list ⇒ list map { obj ⇒
        extract(JsObjectReader.read(obj) \ field)
      } flatten)

    def primitiveOne[A](q: JsObject, field: String)(extract: JsValue ⇒ Option[A]): Fu[Option[A]] =
      coll.find(q, Json.obj(field -> 1)).one map (opt ⇒ opt map { obj ⇒
        extract(JsObjectReader.read(obj) \ field)
      } flatten)
  }

  type ID = String

  //////////////////
  // PRIVATE SHIT //
  //////////////////

  private def cursor(q: JsObject): Cursor[Option[Doc]] = query(q).cursor[Option[Doc]]
  private def cursor(q: JsObject, nb: Int): Cursor[Option[Doc]] = query(q).options(opts batchSize nb).cursor[Option[Doc]]

  private def builder = coll.genericQueryBuilder
  private def query(q: JsObject) = builder query q
  private val opts = QueryOpts()

  private def db = coll.db
  private def name = coll.name

  private implicit val bsonDocumentReader = new BSONDocumentReader[Option[Doc]] {
    def read(bson: BSONDocument): Option[Doc] = json.fromMongo(JsObjectReader read bson).asOpt
  }

  private def fuck(msg: Any) = Future failed (new DbException(msg.toString))
}
