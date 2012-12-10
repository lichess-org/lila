package lila
package mongodb

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

  private implicit val bsonReads = new BSONReader[Doc] {
    def fromBSON(doc: BSONDocument): Doc = json unsafeRead (JsObjectReader fromBSON doc)
  }

  def insert(doc: Doc): Funit = for {
    lastErr ← coll.insert(json write doc)
    result ← lastErr.ok.fold(
      Future successful (),
      Future failed (new RuntimeException(lastErr.message))
    )
  } yield result

  def uncheckedInsert(doc: Doc): Funit = Future successful (coll insert (json write doc))

  def count(query: QueryBuilder): Fu[Int] = db command Count(name, query.makeQueryDocument.some)
  def count: Fu[Int] = db command Count(name, none)

  def findOne(query: QueryBuilder): Fu[Option[Doc]] = cursor(query).headOption

  def find(query: QueryBuilder): Fu[List[Doc]] = cursor(query).toList
  def find(query: QueryBuilder, nb: Int): Fu[List[Doc]] = cursor(query) toList nb

  def cursor(query: QueryBuilder): FlattenedCursor[Doc] = coll.find[Doc](query)

  def query = QueryBuilder()

  ///////////// sorting /////////////
  
  def sortNaturalDesc = BSONDocument("$natural" -> BSONInteger(-1))

  private val coll = db(name)
}
