package lila.db
package api

import Types._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.Implicits._

object update extends update
trait update {

  def apply[ID: Writes, A <: Identified[ID]](doc: A)(implicit coll: Coll, tube: Tube[A]): Funit =
    (tube toMongo doc).fold(fuck(_), js ⇒ apply(select(doc.id), js))

  def apply[A](selector: JsObject, update: JsObject, upsert: Boolean = false, multi: Boolean = false)(implicit coll: Coll): Funit = for {
    lastErr ← coll.update(selector, update, upsert = upsert, multi = multi)
    result ← lastErr.ok.fold(funit, fuck(lastErr.message))
  } yield result

  def doc[ID: Writes, A <: Identified[ID]: Tube](id: ID)(op: A ⇒ JsObject)(implicit coll: Coll): Funit =
    find byId id flatMap { docOption ⇒
      docOption zmap (doc ⇒ update(select(id), op(doc)))
    }

  def field[ID: Writes, A <: Identified[ID]: Tube](id: ID, field: String, value: A)(implicit coll: Coll): Funit =
    update(select(id), $set(field -> value))
}
