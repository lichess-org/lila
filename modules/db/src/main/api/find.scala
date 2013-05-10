package lila.db
package api

import Implicits._

import reactivemongo.bson._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import play.api.libs.json._
import scalaz.{ OptionT, OptionTs }

object $find extends OptionTs {

  def one[A: TubeInColl](
    q: JsObject,
    modifier: QueryBuilder ⇒ QueryBuilder = identity): Fu[Option[A]] =
    one(modifier($query(q)))

  def one[A: TubeInColl](q: QueryBuilder): Fu[Option[A]] =
    q.one[Option[A]] map (_.flatten)

  def byId[ID: Writes, A: TubeInColl](id: ID): Fu[Option[A]] = one($select byId id)
  def byId[A: TubeInColl](id: String): Fu[Option[A]] = byId[String, A](id)

  def byIds[ID: Writes, A: TubeInColl](ids: Iterable[ID]): Fu[List[A]] = apply($select byIds ids)
  def byIds[A: TubeInColl](ids: Iterable[String]): Fu[List[A]] = byIds[String, A](ids)

  def byOrderedIds[ID: Writes, A <: Identified[ID]: TubeInColl](ids: Seq[ID]): Fu[List[A]] =
    byIds(ids) map { docs ⇒
      val docsMap = docs.map(u ⇒ u.id -> u).toMap
      ids.map(docsMap.get).flatten.toList
    }
  def byOrderedIds[A <: Identified[String]: TubeInColl](ids: Seq[String]): Fu[List[A]] =
    byOrderedIds[String, A](ids)

  def all[A: TubeInColl]: Fu[List[A]] = apply($select.all)

  def apply[A: TubeInColl](q: JsObject): Fu[List[A]] =
    $cursor(q).toList map (_.flatten)

  def apply[A: TubeInColl](q: JsObject, nb: Int): Fu[List[A]] =
    $cursor(q) toList nb map (_.flatten)

  def apply[A: TubeInColl](b: QueryBuilder): Fu[List[A]] =
    $cursor(b).toList map (_.flatten)

  def apply[A: TubeInColl](b: QueryBuilder, nb: Int): Fu[List[A]] =
    $cursor(b) toList nb map (_.flatten)

  // useful in capped collections
  def recent[A: TubeInColl](max: Int): Fu[List[A]] = (
    pimpQB($query.all).sort($sort.naturalOrder) 
  ).cursor[Option[A]] toList max map (_.flatten)
}
