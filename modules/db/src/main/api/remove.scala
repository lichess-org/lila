package lila.db
package api

import Types._

import play.api.libs.json._
import play.modules.reactivemongo.Implicits._

object $remove {

  def apply[A: InColl](selector: JsObject): Funit =
    (implicitly[InColl[A]].coll remove selector).void

  def byId[ID: Writes, A: InColl](id: ID): Funit =
    apply($select(id))
  def byId[A: InColl](id: String): Funit = byId(id)

  def byIds[ID: Writes, A: InColl](ids: Seq[ID]): Funit =
    apply($select byIds ids)
  def byIds[A: InColl](ids: Seq[String]): Funit = byIds(ids)

  def apply[ID: Writes, A <: Identified[ID]: TubeInColl](doc: A): Funit =
    byId(doc.id)
  def apply[A <: Identified[String]: TubeInColl](doc: A): Funit = apply(doc)
}
