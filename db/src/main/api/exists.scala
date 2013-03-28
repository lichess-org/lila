package lila.db
package api

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

object exists extends exists
trait exists {

  def apply[A : InColl](q: JsObject): Fu[Boolean] =
    count(q) map (0 !=)

  def byId[ID: Writes, A: InColl](id: ID): Fu[Boolean] =
    apply(select(id))
}
