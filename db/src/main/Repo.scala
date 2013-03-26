package lila.db

import Implicits._

import reactivemongo.api._
import reactivemongo.bson._

import play.modules.reactivemongo.Implicits.{ JsObjectWriter ⇒ _, _ }
import PlayReactiveMongoPatch._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

abstract class Repo[ID: Writes, Doc <: Identified[ID]](
    json: JsonTube[Doc])(implicit val coll: ReactiveColl) extends api.Full {

  def >[A](op: ReactiveColl ⇒ A) = op(coll)

  object insert {

    def apply(doc: Doc): Funit = (json toMongo doc).fold(fuck(_), apply(_))

    def apply(js: JsObject): Funit = coll insert js flatMap { lastErr ⇒
      lastErr.ok.fold(funit, fuck(lastErr.message))
    }

    def unchecked(doc: Doc) {
      json toMongo doc foreach { coll.insert(_) }
    }
  }

  object update {

    def apply(doc: Doc): Funit = (json toMongo doc).fold(
      fuck(_),
      js ⇒ apply(select(doc.id), js)
    )

    def apply(selector: JsObject, update: JsObject, upsert: Boolean = false, multi: Boolean = false): Funit = for {
      lastErr ← coll.update(selector, update, upsert = upsert, multi = multi)
      result ← lastErr.ok.fold(funit, fuck(lastErr.message))
    } yield result

    def unchecked(selector: JsObject, update: JsObject, upsert: Boolean = false, multi: Boolean = false) {
      coll.uncheckedUpdate(selector, update, upsert = upsert, multi = multi)
    }

    def doc(id: ID)(op: Doc ⇒ JsObject): Funit =
      find byId id flatMap { docOption ⇒
        docOption zmap (doc ⇒ update(select(id), op(doc)))
      }

    def field[A: Writes](id: ID, field: String, value: A) =
      update(select(id), $set(field -> value))
  }

  object remove {

    def apply(selector: JsObject): Funit = (coll remove selector).void

    def byId(id: ID): Funit = apply(select(id))

    def byIds(ids: Seq[ID]): Funit = apply(select byIds ids)
  }

  //////////////////
  // PRIVATE SHIT //
  //////////////////

  protected implicit val bsonDocumentReader = new BSONDocumentReader[Option[Doc]] {
    def read(bson: BSONDocument): Option[Doc] = json.fromMongo(JsObjectReader read bson).asOpt
  }

  private def cursor(q: JsObject): Cursor[Option[Doc]] = cursor(query(q))
  private def cursor(q: JsObject, nb: Int): Cursor[Option[Doc]] = cursor(query(q), nb)

  private def cursor(b: QueryBuilder): Cursor[Option[Doc]] = b.cursor[Option[Doc]]
  private def cursor(b: QueryBuilder, nb: Int): Cursor[Option[Doc]] = cursor(b limit nb)
  private val opts = QueryOpts()

  private def db = coll.db
  private def name = coll.name

  private def fuck(msg: Any) = fufail(new DbException(msg.toString))
}
