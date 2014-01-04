package lila.db
package api

import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.bson._
import Types._

object $query {

  def all[A: InColl] = builder

  def apply[A: InColl](q: JsObject) = builder query q

  def apply[A: InColl](q: BSONDocument) = builder query q

  def byId[A: InColl, B: Writes](id: B) = apply($select byId id)

  def byIds[A: InColl, B: Writes](ids: Iterable[B]) = apply($select byIds ids)

  def builder[A: InColl] = implicitly[InColl[A]].coll.genericQueryBuilder
}
