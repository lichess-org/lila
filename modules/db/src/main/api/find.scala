package lila.db
package api

import Implicits._
import play.api.libs.json._
import reactivemongo.bson._
import reactivemongo.api._

object $find {

  def one[A: TubeInColl](
    q: JsObject,
    modifier: QueryBuilder => QueryBuilder = identity): Fu[Option[A]] =
    one(modifier($query(q)))

  def one[A: TubeInColl](q: QueryBuilder): Fu[Option[A]] =
    q.one[Option[A]] map (_.flatten)

  def byId[ID: Writes, A: TubeInColl](id: ID): Fu[Option[A]] = one($select byId id)
  def byId[A: TubeInColl](id: String): Fu[Option[A]] = byId[String, A](id)

  def byIds[ID: Writes, A: TubeInColl](ids: Iterable[ID]): Fu[List[A]] = apply($select byIds ids)
  def byIds[A: TubeInColl](ids: Iterable[String]): Fu[List[A]] = byIds[String, A](ids)

  def byOrderedIds[ID: Writes, A <: Identified[ID]: TubeInColl](ids: Iterable[ID]): Fu[List[A]] =
    byIds(ids) map { docs =>
      val docsMap = docs.map(u => u.id -> u).toMap
      ids.flatMap(docsMap.get).toList
    }
  def byOrderedIds[A <: Identified[String]: TubeInColl](ids: Iterable[String]): Fu[List[A]] =
    byOrderedIds[String, A](ids)

  def optionsByOrderedIds[ID: Writes, A <: Identified[ID]: TubeInColl](ids: Iterable[ID]): Fu[List[Option[A]]] =
    byIds(ids) map { docs =>
      val docsMap = docs.map(u => u.id -> u).toMap
      ids.map(docsMap.get).toList
    }
  def opByOrderedIds[A <: Identified[String]: TubeInColl](ids: Iterable[String]): Fu[List[Option[A]]] =
    optionsByOrderedIds[String, A](ids)

  def all[A: TubeInColl]: Fu[List[A]] = apply($select.all)

  def apply[A: TubeInColl](q: JsObject): Fu[List[A]] =
    $query(q).toList[Option[A]](none) map (_.flatten)

  def apply[A: TubeInColl](q: JsObject, nb: Int): Fu[List[A]] =
    $query(q).toList[Option[A]](nb.some) map (_.flatten)

  def apply[A: TubeInColl](b: QueryBuilder): Fu[List[A]] =
    b.toList[Option[A]](none) map (_.flatten)

  def apply[A: TubeInColl](b: QueryBuilder, nb: Int): Fu[List[A]] =
    b.toList[Option[A]](nb.some) map (_.flatten)

  def apply[A: TubeInColl](b: QueryBuilder, nb: Int, readPreference: ReadPreference): Fu[List[A]] =
    b.toList[Option[A]](nb.some, readPreference) map (_.flatten)
}
