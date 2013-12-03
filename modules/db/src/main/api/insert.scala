package lila.db
package api

import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.bson._
import Types.Coll

object $insert {

  def apply[A: JsTubeInColl](doc: A): Funit =
    (implicitly[JsTube[A]] toMongo doc).fold(e ⇒ fufail(e.toString), apply(_))

  def apply[A: InColl](js: JsObject): Funit = successful {
    implicitly[InColl[A]].coll insert js
  }

  def bson[A: BsTubeInColl](doc: A): Funit = bson {
    implicitly[BsTube[A]].handler write doc
  }

  def bson[A: InColl](bs: BSONDocument): Funit = successful {
    implicitly[InColl[A]].coll insert bs
  }
}
