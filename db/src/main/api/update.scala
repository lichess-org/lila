package lila.db
package api

import Types._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.Implicits._

object update extends update
trait update {

  def apply[ID: Writes, A <: Identified[ID] : TubeInColl](doc: A): Funit =
    (implicitly[Tube[A]] toMongo doc).fold(
      fuck(_), 
      js ⇒ apply(select(doc.id), js)
    )

  def apply(selector: JsObject, update: JsObject, upsert: Boolean = false, multi: Boolean = false)(implicit inColl: InColl[_]): Funit = for {
    lastErr ← inColl.coll.update(selector, update, upsert = upsert, multi = multi)
    result ← lastErr.ok.fold(funit, fuck(lastErr.message))
  } yield result

  def doc[ID: Writes, A <: Identified[ID]: TubeInColl](id: ID)(op: A ⇒ JsObject): Funit =
    find byId id flatMap { docOption ⇒
      docOption zmap (doc ⇒ update(select(id), op(doc)))
    }

  def field[ID: Writes, A: Writes](id: ID, field: String, value: A)(implicit inColl: InColl[_]): Funit =
    update(select(id), $set(field -> value))
}
