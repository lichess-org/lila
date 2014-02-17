package lila.db
package api

import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import Types._

object $save {

  def apply[ID: Writes, A <: Identified[ID]: JsTubeInColl](doc: A): Funit =
    (implicitly[JsTube[A]] toMongo doc).fold(e => fufail(e.toString),
      js => $update($select(doc.id), js, upsert = true)
    )

  def apply[A <: Identified[String]: JsTubeInColl](doc: A): Funit = apply[String, A](doc)

  def apply[ID: Writes, A: InColl](id: ID, doc: JsObject): Funit =
    $update($select(id), doc + ("_id" -> Json.toJson(id)), upsert = true, multi = false)
}
