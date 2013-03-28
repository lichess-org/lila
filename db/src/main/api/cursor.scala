package lila.db
package api

import Implicits._

import play.api.libs.json._

import reactivemongo.api._
import reactivemongo.bson._
import play.api.libs.concurrent.Execution.Implicits._

object $cursor {

  def apply[A: TubeInColl](q: JsObject): Cursor[Option[A]] =
    apply($query(q))

  def apply[A: TubeInColl](q: JsObject, nb: Int): Cursor[Option[A]] =
    apply($query(q), nb)

  def apply[A: TubeInColl](b: QueryBuilder): Cursor[Option[A]] =
    b.cursor[Option[A]]

  def apply[A: TubeInColl](b: QueryBuilder, nb: Int): Cursor[Option[A]] =
    apply(b limit nb)
}
