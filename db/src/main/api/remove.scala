package lila.db
package api

import Types.Coll

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.Implicits._

object $remove {

  def apply[A: InColl](selector: JsObject): Funit =
    (implicitly[InColl[A]].coll remove selector).void

  def byId[ID: Writes, A: InColl](id: ID): Funit =
    apply($select(id))

  def byIds[ID: Writes, A: InColl](ids: Seq[ID]): Funit =
    apply($select byIds ids)
}
