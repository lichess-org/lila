package lila.db

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands._
import reactivemongo.bson._
import reactivemongo.bson.DefaultBSONHandlers._
import reactivemongo.api.collections.default.BSONCollection

import play.modules.reactivemongo._
import play.modules.reactivemongo.Implicits._

import play.api.libs.json._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

abstract class Coll[Doc <: WithStringId](coll: BSONCollection, json: JsonTube[Doc]) {

  type ID = String

  // def count(q: QueryBuilder): Fu[Int] = db command Count(name, q.makeQueryDocument.some)
  // def count: Fu[Int] = db command Count(name, none)

  // def findOne(q: QueryBuilder): Fu[Option[Doc]] =
  //   cursor(q, query.o batchSize 1).headOption map (_.flatten)

  // def byId(id: ID): Fu[Option[Doc]] = findOne(query byId id)

  // def byIds(ids: Seq[ID]): Fu[List[Doc]] = find(query byIds ids)

  // def byOrderedIds(ids: Seq[ID]): Fu[List[Doc]] = byIds(ids) map { docs ⇒
  //   val docsMap = docs.map(u ⇒ u.id -> u).toMap
  //   ids.map(docsMap.get).flatten.toList
  // }

  def find(q: JsObject): Fu[List[Doc]] = cursor(q).toList map (_.flatten)

  def find(q: JsObject, nb: Int): Fu[List[Doc]] =
    query(q).options(query.opts batchSize nb).cursor[Option[Doc]] toList nb map (_.flatten)

  def cursor(q: JsObject): Cursor[Option[Doc]] = query(q).cursor[Option[Doc]]

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

  // object projection {

  //   def primitive[A](
  //     q: QueryBuilder,
  //     field: String,
  //     opts: QueryOpts = query.o)(extract: JsValue ⇒ Option[A]): Fu[List[A]] =
  //     coll.find[BSONDocument](q projection Json.obj(field -> 1), opts).toList map (list ⇒ list map { obj ⇒
  //       extract(JsObjectReader.fromBSON(obj) \ field)
  //     } flatten)

  //   def primitiveOne[A](
  //     q: QueryBuilder,
  //     field: String,
  //     opts: QueryOpts = query.o)(extract: JsValue ⇒ Option[A]): Fu[Option[A]] =
  //     primitive(q, field, opts batchSize 1)(extract) map (_.headOption)
  // }

  //////////////////
  // PRIVATE SHIT //
  //////////////////

  protected lazy val query = Query(coll.genericQueryBuilder)

  private def db = coll.db
  private def name = coll.name

  private implicit val bsonReads = new BSONDocumentReader[Option[Doc]] {
    def read(bson: BSONDocument): Option[Doc] =
      json.fromMongo(JsObjectReader read bson).pp.asOpt
  }

  private def fuck(msg: Any) = Future failed (new DbException(msg.toString))
}
