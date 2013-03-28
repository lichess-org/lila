package lila.db
package api

import Types._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.Implicits._

object $update {

  def apply[ID: Writes, A <: Identified[ID]: TubeInColl](doc: A): Funit =
    (implicitly[Tube[A]] toMongo doc).fold(
      fuck(_),
      js ⇒ apply($select(doc.id), js)
    )

  def apply[A: InColl](selector: JsObject, update: JsObject, upsert: Boolean = false, multi: Boolean = false): Funit = for {
    lastErr ← implicitly[InColl[A]].coll.update(selector, update, upsert = upsert, multi = multi)
    result ← lastErr.ok.fold(funit, fuck(lastErr.message))
  } yield result

  def doc[ID: Writes, A <: Identified[ID]: TubeInColl](id: ID)(op: A ⇒ JsObject): Funit =
    $find byId id flatMap { docOption ⇒
      docOption zmap (doc ⇒ $update($select(id), op(doc)))
    }

  def field[ID: Writes, A: InColl, B: Writes](id: ID, field: String, value: B): Funit =
    $update($select(id), $set(field -> value))
}
