package lila.db
package api

import Types.Coll

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._
import play.modules.reactivemongo.Implicits._

object remove extends remove
trait remove {

  def apply(selector: JsObject)(implicit coll: Coll): Funit = (coll remove selector).void

  def byId[ID: Writes](id: ID)(implicit coll: Coll): Funit = apply(select(id))

  def byIds[ID: Writes](ids: Seq[ID])(implicit coll: Coll): Funit = apply(select byIds ids)
}
