package lila.db
package api

import Types.Coll

import reactivemongo.bson._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import play.api.libs.json._

object $insert {

  def apply[A: TubeInColl](doc: A): Funit =
    (implicitly[Tube[A]] toMongo doc).fold(e ⇒ fufail(e.toString), apply(_))

  def apply[A: InColl](js: JsObject): Funit = successful {
    implicitly[InColl[A]].coll insert js
  }
}
