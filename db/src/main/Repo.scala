package lila.db

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands._

import play.modules.reactivemongo.Implicits._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

abstract class Repo[Doc <: WithStringId](coll: ReactiveColl, json: JsonTube[Doc]) extends DbApi {

  object query {

    def apply(q: JsObject) = builder query q

    def all = builder

    def byIds(ids: Seq[ID]) = apply(select byIds ids)
  }

  object count {

    def apply(q: JsObject): Fu[Int] = db command Count(name, JsObjectWriter.write(q).some)

    def apply: Fu[Int] = db command Count(name, none)
  }

  object exists {

    def apply(q: JsObject): Fu[Boolean] = count(q) map (0 !=)

    def byId(id: ID): Fu[Boolean] = apply(select(id))
  }

  object find {

    def one(q: JsObject): Fu[Option[Doc]] = query(q).one[Option[Doc]] map (_.flatten)

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

  object insert {

    def apply(doc: Doc): Funit = (json toMongo doc).fold(fuck(_), apply(_))

    def apply(js: JsObject): Funit = coll insert js flatMap { lastErr ⇒
      lastErr.ok.fold(funit, fuck(lastErr.message))
    }

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

    def doc(id: String)(op: Doc ⇒ JsObject): Funit =
      find byId id flatMap { docOption ⇒ ~docOption.map(doc ⇒ update(select(id), op(doc))) }

    def field[A : Writes](id: String, field: String, value: A) = 
      update(select(id), $set(field -> value))
  }

  object remove {

    def apply(selector: JsObject): Funit = (coll remove selector).void
  }

  object primitive {

    def apply[A](q: JsObject, field: String, modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsValue ⇒ Option[A]): Fu[List[A]] =
      modifier(coll.find(q, Json.obj(field -> 1))).cursor.toList map (list ⇒ list map { obj ⇒
        extract(JsObjectReader.read(obj) \ field)
      } flatten)

    def one[A](q: JsObject, field: String, modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsValue ⇒ Option[A]): Fu[Option[A]] =
      modifier(coll.find(q, Json.obj(field -> 1))).one map (opt ⇒ opt map { obj ⇒
        extract(JsObjectReader.read(obj) \ field)
      } flatten)
  }

  object projection {

    def apply[A](q: JsObject, fields: Seq[String], modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsObject ⇒ Option[A]): Fu[List[A]] =
      modifier(coll.find(q, projector(fields))).cursor.toList map (list ⇒ list map { obj ⇒
        extract(JsObjectReader read obj)
      } flatten)

    def one[A](q: JsObject, fields: Seq[String], modifier: QueryBuilder ⇒ QueryBuilder = identity)(extract: JsObject ⇒ Option[A]): Fu[Option[A]] =
      modifier(coll.find(q, projector(fields))).one map (opt ⇒ opt map { obj ⇒
        extract(JsObjectReader read obj)
      } flatten)

    private def projector(fields: Seq[String]): JsObject = Json obj {
      (fields map (_ -> Json.toJsFieldJsValueWrapper(1))): _*
    }
  }

  type ID = String

  //////////////////
  // PRIVATE SHIT //
  //////////////////

  private def cursor(q: JsObject): Cursor[Option[Doc]] = cursor(query(q))
  private def cursor(q: JsObject, nb: Int): Cursor[Option[Doc]] = cursor(query(q), nb)

  private def cursor(b: QueryBuilder): Cursor[Option[Doc]] = b.cursor[Option[Doc]]
  private def cursor(b: QueryBuilder, nb: Int): Cursor[Option[Doc]] = cursor(b limit nb)

  private def builder = coll.genericQueryBuilder
  private val opts = QueryOpts()

  private def db = coll.db
  private def name = coll.name

  private implicit val bsonDocumentReader = new BSONDocumentReader[Option[Doc]] {
    def read(bson: BSONDocument): Option[Doc] = json.fromMongo(JsObjectReader read bson).asOpt
  }

  private def fuck(msg: Any) = fufail(new DbException(msg.toString))
}
