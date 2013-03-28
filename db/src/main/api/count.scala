package lila.db
package api

import Types._

import play.api.libs.json.JsObject
import reactivemongo.core.commands.Count
import play.modules.reactivemongo.Implicits._
import play.api.libs.concurrent.Execution.Implicits._

object count extends count
trait count {

  def apply[A: InColl](q: JsObject): Fu[Int] =
    implicitly[InColl[A]].coll |> { coll ⇒
      coll.db command Count(coll.name, JsObjectWriter.write(q).some)
    }

  def apply[A: InColl]: Fu[Int] =
    implicitly[InColl[A]].coll |> { coll ⇒
      coll.db command Count(coll.name, none)
    }
}
