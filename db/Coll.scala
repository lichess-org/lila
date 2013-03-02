package lila.db

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands._
import reactivemongo.bson.handlers._
import reactivemongo.bson.handlers.DefaultBSONHandlers._

import play.modules.reactivemongo._
import play.modules.reactivemongo.PlayBsonImplicits._

import play.api.libs.json._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

abstract class Coll[Doc <: WithStringId](
  db: LilaDB, 
  name: String, 
  json: JsonTube[Doc]
) {

  type ID = String

  def count(q: QueryBuilder): Fu[Int] = db command Count(name, q.makeQueryDocument.some)
  def count: Fu[Int] = db command Count(name, none)

  def findOne(q: QueryBuilder): Fu[Option[Doc]] =
    cursor(q, query.o batchSize 1).headOption map (_.flatten)

  def byId(id: ID): Fu[Option[Doc]] = findOne(query byId id)

  def byIds(ids: Seq[ID]): Fu[List[Doc]] = find(query byIds ids)

  def byOrderedIds(ids: Seq[ID]): Fu[List[Doc]] = byIds(ids) map { docs ⇒
    val docsMap = docs.map(u ⇒ u.id -> u).toMap
    ids.map(docsMap.get).flatten.toList
  }

  def find(q: QueryBuilder, opts: QueryOpts = query.o): Fu[List[Doc]] =
    cursor(q, opts).toList map (_.flatten)

  def find(q: QueryBuilder, nb: Int): Fu[List[Doc]] =
    cursor(q, query.o batchSize nb) toList nb map (_.flatten)

  def cursor(q: QueryBuilder, opts: QueryOpts = query.o): FlattenedCursor[Option[Doc]] =
    coll.find[Option[Doc]](q, opts)

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

    def primitive[A](
      q: QueryBuilder,
      field: String,
      opts: QueryOpts = query.o)(extract: JsValue ⇒ Option[A]): Fu[List[A]] =
      coll.find[BSONDocument](q projection Json.obj(field -> 1), opts).toList map (list ⇒ list map { obj ⇒
        extract(JsObjectReader.fromBSON(obj) \ field)
      } flatten)

    def primitiveOne[A](
      q: QueryBuilder,
      field: String,
      opts: QueryOpts = query.o)(extract: JsValue ⇒ Option[A]): Fu[Option[A]] =
      primitive(q, field, opts batchSize 1)(extract) map (_.headOption)
  }

  //////////////////
  // PRIVATE SHIT //
  //////////////////

  private implicit val bsonReads = new BSONReader[Option[Doc]] {
    def fromBSON(bson: BSONDocument): Option[Doc] =
      json.fromMongo(JsObjectReader fromBSON bson).pp.asOpt
  }

  private val coll = db(name)

  private def fuck(msg: Any) = Future failed (new DbException(msg.toString))
}
