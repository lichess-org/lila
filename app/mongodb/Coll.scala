package lila
package mod

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands._
import reactivemongo.bson.handlers._
import reactivemongo.bson.handlers.DefaultBSONHandlers._

// Reactive Mongo plugin
import play.modules.reactivemongo._
import play.modules.reactivemongo.PlayBsonImplicits._

// Play Json imports
import play.api.libs.json._
import play.api.libs.functional.syntax._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

abstract class Coll[Doc](db: LilaDB, name: String, json: JsonTube[Doc]) {

  private type F[A] = Future[A]

  private implicit val bsonReads = new BSONReader[Doc] {
    def fromBSON(doc: BSONDocument): Doc = json unsafeRead (JsObjectReader fromBSON doc)
  }

  def insert(doc: Doc): F[Unit] = for {
    lastErr ← coll.insert(json write doc)
    result ← lastErr.ok.fold(
      Future successful (),
      Future failed (new RuntimeException(lastErr.message))
    )
  } yield result

  def count(query: QueryBuilder): F[Int] = db command Count(name, query.makeQueryDocument.some)
  def count: F[Int] = db command Count(name, none)

  def findOne(query: QueryBuilder): F[Option[Doc]] = coll.find[Doc](query).headOption

  def find(query: QueryBuilder): F[List[Doc]] = coll.find[Doc](query).toList
  def find(query: QueryBuilder, nb: Int): F[List[Doc]] = coll.find[Doc](query) toList nb

  def recent(nb: Int): F[List[Doc]] =
    find(QueryBuilder().sort("$natural" -> SortOrder.Descending), nb)

  private val coll = db(name)
}
