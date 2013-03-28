package lila.db
package api

import Types.Coll

import play.api.libs.json.JsObject
import reactivemongo.core.commands.Count
import play.modules.reactivemongo.Implicits._
import play.api.libs.concurrent.Execution.Implicits._

object count extends count
trait count {

  def apply(q: JsObject)(implicit coll: Coll): Fu[Int] =
    coll.db command Count(coll.name, JsObjectWriter.write(q).some)

  def apply(implicit coll: Coll): Fu[Int] =
    coll.db command Count(coll.name, none)
}
