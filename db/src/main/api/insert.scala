package lila.db
package api

import Types.Coll

import reactivemongo.bson._
import play.modules.reactivemongo.Implicits._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

object insert extends insert
trait insert {

  def apply[A](doc: A)(implicit coll: Coll, json: Tube[A]): Funit =
    (json toMongo doc).fold(fuck(_), apply(_))

  def apply(js: JsObject)(implicit coll: Coll): Funit =
    coll insert js flatMap { lastErr ⇒
      lastErr.ok.fold(funit, fuck(lastErr.message))
    }
}
