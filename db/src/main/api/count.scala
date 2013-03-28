package lila.db
package api

import Types._

import play.api.libs.json.JsObject
import reactivemongo.core.commands.Count
import play.modules.reactivemongo.Implicits._
import play.api.libs.concurrent.Execution.Implicits._

object count extends count
trait count {

  def apply(q: JsObject)(implicit inColl: InColl[_]): Fu[Int] =
    inColl.coll.db command Count(inColl.coll.name, JsObjectWriter.write(q).some)

  def apply(implicit inColl: InColl[_]): Fu[Int] =
    inColl.coll.db command Count(inColl.coll.name, none)
}
