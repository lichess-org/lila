package lila.db
package api

import Types.Coll

import reactivemongo.bson._
import play.modules.reactivemongo.Implicits._
import play.api.libs.json._

object $insert {

  def apply[A : TubeInColl](doc: A): Funit =
    (implicitly[Tube[A]] toMongo doc).fold(fuck(_), apply(_))

  def apply[A : InColl](js: JsObject): Funit =
    implicitly[InColl[A]].coll insert js flatMap { lastErr ⇒
      lastErr.ok.fold(funit, fuck(lastErr.message))
    }
}
