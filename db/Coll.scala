package lila.db

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands._
import reactivemongo.bson.handlers._
import reactivemongo.bson.handlers.DefaultBSONHandlers._

import play.modules.reactivemongo._
import play.modules.reactivemongo.PlayBsonImplicits._

import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

abstract class Coll[Doc <: WithStringId](db: LilaDB, name: String, json: JsonTube[Doc]) {

  type ID = String

  def insert(doc: Doc): Funit = (json toMongo doc).fold(fuck(_), js ⇒ for {
    lastErr ← coll insert js
    result ← lastErr.ok.fold(funit, fuck(lastErr.message))
  } yield result)

  def uncheckedInsert(doc: Doc): Funit = {
    Future { json toMongo doc foreach { coll.insert(_) } }
    funit
  }

  def count(q: QueryBuilder): Fu[Int] = db command Count(name, q.makeQueryDocument.some)
  def count: Fu[Int] = db command Count(name, none)

  def findOne(q: QueryBuilder): Fu[Option[Doc]] = 
    cursor(q, query.o batchSize 1).headOption map (_.flatten)

  def byId(id: ID): Fu[Option[Doc]] = findOne(query byId id)

  def byIds(ids: Iterable[ID]): Fu[List[Doc]] = find(query byIds ids)

  def byOrderedIds(ids: Iterable[ID]): Fu[List[Doc]] = byIds(ids) map { docs ⇒
    val docsMap = docs.map(u ⇒ u.id -> u).toMap
    ids.map(docsMap.get).flatten.toList
  }

  def find(q: QueryBuilder, opts: QueryOpts = query.o): Fu[List[Doc]] = 
    cursor(q, opts).toList map (_.flatten)

  def find(q: QueryBuilder, nb: Int): Fu[List[Doc]] = 
    cursor(q, query.o batchSize nb) toList nb map (_.flatten)

  def cursor(q: QueryBuilder, opts: QueryOpts = query.o): FlattenedCursor[Option[Doc]] = 
    coll.find[Option[Doc]](q, opts)

  object projection {
    
    // def primitive[A](q: QueryBuilder, pjn: String, opts: QueryOpts = query.o): Fu[List[A]] =
    //   coll.find[A](q, Json.obj(pjn -> true), opts)
  }

  object query {

    val q = QueryBuilder()
    val o = QueryOpts()

    def apply(js: JsObject) = q query js

    def byId(id: ID) = q query Json.obj("_id" -> id)

    def byIds(ids: Iterable[ID]): QueryBuilder = q query Json.obj(
      "_id" -> Json.obj("$in" -> Json.arr(ids))
    )

    def sorted = q sort sort.naturalDesc
  }

  object sort {

    def naturalDesc = ("$natural" -> desc)

    def asc = SortOrder.Ascending
    def desc = SortOrder.Descending
  }

  //////////////////
  // PRIVATE SHIT //
  //////////////////

  private implicit val bsonReads = new BSONReader[Option[Doc]] {
    def fromBSON(bson: BSONDocument): Option[Doc] =
      json.read(JsObjectReader fromBSON bson.pp).asOpt
  }

  private val coll = db(name)

  private def fuck(msg: Any) = Future failed (new DbException(msg.toString))
}
