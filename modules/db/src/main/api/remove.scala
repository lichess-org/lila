package lila.db
package api

import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import Types._

object $remove {

  def apply[A: InColl](selector: JsObject): Funit =
    implicitly[InColl[A]].coll remove selector void

  def byId[ID: Writes, A: InColl](id: ID): Funit =
    apply($select(id))
  def byId[A: InColl](id: String): Funit = byId[String, A](id)

  def byIds[ID: Writes, A: InColl](ids: Seq[ID]): Funit =
    apply($select byIds ids)
  def byIds[A: InColl](ids: Seq[String]): Funit = byIds[String, A](ids)

  def apply[ID: Writes, A <: Identified[ID]: TubeInColl](doc: A): Funit =
    byId(doc.id)
  def apply[A <: Identified[String]: TubeInColl](doc: A): Funit = apply[String, A](doc)
}
