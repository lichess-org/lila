package lila.db
package api

import Implicits._

import play.api.libs.json._

import reactivemongo.api._
import reactivemongo.bson._
import play.api.libs.concurrent.Execution.Implicits._

object cursor extends cursor
trait cursor {

  def apply[A: Tube](q: JsObject)(implicit coll: Coll): Cursor[Option[A]] =
    apply(query(q))

  def apply[A: Tube](q: JsObject, nb: Int)(implicit coll: Coll): Cursor[Option[A]] =
    apply(query(q), nb)

  def apply[A: Tube](b: QueryBuilder)(implicit coll: Coll): Cursor[Option[A]] =
    b.cursor[Option[A]]

  def apply[A: Tube](b: QueryBuilder, nb: Int)(implicit coll: Coll): Cursor[Option[A]] =
    apply(b limit nb)
}
