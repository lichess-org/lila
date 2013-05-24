package lila.db
package api

import Implicits._
import play.api.libs.json._
import reactivemongo.api._
import reactivemongo.bson._

object $cursor {

  def apply[A: TubeInColl](q: JsObject): Cursor[Option[A]] =
    apply($query(q))

  def apply[A: TubeInColl](b: QueryBuilder): Cursor[Option[A]] =
    b.cursor[Option[A]]
}
