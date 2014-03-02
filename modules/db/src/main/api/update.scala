package lila.db
package api

import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.bson._
import Types._

object $update {

  def apply[ID: Writes, A <: Identified[ID]: JsTubeInColl](doc: A): Funit =
    (implicitly[JsTube[A]] toMongo doc).fold(e => fufail(e.toString),
      js => apply($select(doc.id), js)
    )
  def apply[A <: Identified[String]: JsTubeInColl](doc: A): Funit = apply[String, A](doc)

  def apply[A: InColl, B: BSONDocumentWriter](selector: JsObject, update: B, upsert: Boolean = false, multi: Boolean = false): Funit =
      implicitly[InColl[A]].coll.update(selector, update, upsert = upsert, multi = multi).void

  def doc[ID: Writes, A <: Identified[ID]: TubeInColl](id: ID)(op: A => JsObject): Funit =
    $find byId id flatten "[db] cannot update missing doc" flatMap { doc =>
      apply($select(id), op(doc))
    }

  def docBson[ID: Writes, A <: Identified[ID]: TubeInColl](id: ID)(op: A => BSONDocument): Funit =
    $find byId id flatten "[db] cannot update missing doc" flatMap { doc =>
      apply($select(id), op(doc))
    }

  def field[ID: Writes, A: InColl, B: Writes](id: ID, name: String, value: B, upsert: Boolean = false): Funit =
    apply($select(id), $set(name -> value), upsert = upsert)

  def bsonField[ID: Writes, A: InColl](id: ID, name: String, value: BSONValue, upsert: Boolean = false): Funit =
    apply($select(id), BSONDocument("$set" -> BSONDocument(name -> value)), upsert = upsert)

  // UNCHECKED

  def unchecked[A: InColl](selector: JsObject, update: JsObject, upsert: Boolean = false, multi: Boolean = false) {
    implicitly[InColl[A]].coll.uncheckedUpdate(selector, update, upsert, multi)
  }

  def fieldUnchecked[ID: Writes, A: InColl, B: Writes](id: ID, name: String, value: B, upsert: Boolean = false) {
    unchecked($select(id), $set(name -> value), upsert = upsert)
  }
}
