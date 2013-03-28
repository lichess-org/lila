package lila.db
package api

import Types._

import reactivemongo.bson._
import play.modules.reactivemongo.Implicits._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

object query extends query
trait query {

  def all[A: InColl] = builder

  def apply[A: InColl](q: JsObject) = builder query q

  def byId[A: InColl, B: Writes](id: B) = apply(select byId id)

  def byIds[A: InColl, B: Writes](ids: Seq[B]) = apply(select byIds ids)

  def builder[A: InColl] = implicitly[InColl[A]].coll.genericQueryBuilder
}
