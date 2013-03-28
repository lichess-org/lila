package lila.db
package api

import Types.Coll

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

object exists extends exists
trait exists {

  private object count extends count

  def apply(q: JsObject)(implicit coll: Coll): Fu[Boolean] =
    count(q) map (0 !=)

  def byId[A: Writes](id: A)(implicit coll: Coll): Fu[Boolean] =
    apply(select(id))
}
