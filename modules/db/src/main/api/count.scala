package lila.db
package api

import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers.JsObjectWriter
import reactivemongo.core.commands.Count
import Types._

object $count {

  def apply[A: InColl](q: JsObject): Fu[Int] =
    implicitly[InColl[A]].coll |> { coll =>
      coll.db command Count(coll.name, JsObjectWriter.write(q).some)
    }

  def apply[A: InColl]: Fu[Int] =
    implicitly[InColl[A]].coll |> { coll =>
      coll.db command Count(coll.name, none)
    }

  def exists[A : InColl](q: JsObject): Fu[Boolean] = apply(q) map (0 !=)

  def exists[ID: Writes, A: InColl](id: ID): Fu[Boolean] = exists($select(id))
  def exists[A: InColl](id: String): Fu[Boolean] = exists[String, A](id)
}
