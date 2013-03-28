package lila.db
package api

import Implicits._

import reactivemongo.bson._
import play.modules.reactivemongo.Implicits._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

object find extends find
trait find {

  def one[A: Tube](
    q: JsObject,
    modifier: QueryBuilder ⇒ QueryBuilder = identity)(implicit coll: Coll): Fu[Option[A]] =
    modifier(query(q)).one[Option[A]] map (_.flatten)

  def byId[ID: Writes, A: Tube](id: ID)(implicit coll: Coll): Fu[Option[A]] =
    one(select byId id)

  def byIds[ID: Writes, A: Tube](ids: Seq[ID])(implicit coll: Coll): Fu[List[A]] =
    apply(select byIds ids)

  def byOrderedIds[ID: Writes, A <: Identified[ID]: Tube](ids: Seq[ID])(implicit coll: Coll): Fu[List[A]] =
    byIds(ids) map { docs ⇒
      val docsMap = docs.map(u ⇒ u.id -> u).toMap
      ids.map(docsMap.get).flatten.toList
    }

  def all[A: Tube](implicit coll: Coll): Fu[List[A]] = apply(select.all)

  def apply[A: Tube](q: JsObject)(implicit coll: Coll): Fu[List[A]] =
    cursor(q).toList map (_.flatten)

  def apply[A: Tube](q: JsObject, nb: Int)(implicit coll: Coll): Fu[List[A]] =
    cursor(q, nb) toList nb map (_.flatten)

  def apply[A: Tube](b: QueryBuilder)(implicit coll: Coll): Fu[List[A]] =
    cursor(b).toList map (_.flatten)

  def apply[A: Tube](b: QueryBuilder, nb: Int)(implicit coll: Coll): Fu[List[A]] =
    cursor(b, nb) toList nb map (_.flatten)

  // useful in capped collections
  def recent[A: Tube](max: Int)(implicit coll: Coll): Fu[List[A]] = (
    pimpQB(query.all).sort(sort.naturalOrder) limit max
  ).cursor[Option[A]].toList map (_.flatten)
}
