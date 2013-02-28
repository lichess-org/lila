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

abstract class Coll[Doc](db: LilaDB, name: String, json: JsonTube[Doc]) {

  private implicit val bsonReads = new BSONReader[Option[Doc]] {
    def fromBSON(bson: BSONDocument): Option[Doc] = 
      json.read(JsObjectReader fromBSON bson).asOpt
  }

  def insert(doc: Doc): Funit = (json toMongo doc).fold(fuck(_), js ⇒ for {
    lastErr ← coll insert js
    result ← lastErr.ok.fold(funit, fuck(lastErr.message))
  } yield result)

  def uncheckedInsert(doc: Doc): Funit = insert(doc) fallbackTo funit

  def count(query: QueryBuilder): Fu[Int] = db command Count(name, query.makeQueryDocument.some)
  def count: Fu[Int] = db command Count(name, none)

  def findOne(query: QueryBuilder): Fu[Option[Doc]] = cursor(query).headOption map (_.flatten)

  def find(query: QueryBuilder): Fu[List[Doc]] = cursor(query).toList map (_.flatten)
  def find(query: QueryBuilder, nb: Int): Fu[List[Doc]] = cursor(query) toList nb map (_.flatten)

  def cursor(query: QueryBuilder): FlattenedCursor[Option[Doc]] = 
    coll.find[Option[Doc]](query)

  def query = QueryBuilder()

  ///////////// sorting /////////////

  def sortNaturalDesc = BSONDocument("$natural" -> BSONInteger(-1))

  private val coll = db(name)

  private def fuck(msg: Any) = Future failed (new DbException(msg.toString))
}
