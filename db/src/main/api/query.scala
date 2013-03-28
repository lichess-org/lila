package lila.db
package api

import Types.Coll

import reactivemongo.bson._
import play.modules.reactivemongo.Implicits._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

object query extends query
trait query {

  def all(implicit coll: Coll) = builder

  def apply(q: JsObject)(implicit coll: Coll) = builder query q

  def byId[A: Writes](id: A)(implicit coll: Coll) = apply(select byId id)

  def byIds[A: Writes](ids: Seq[A])(implicit coll: Coll) = apply(select byIds ids)

  def builder(implicit coll: Coll) = coll.genericQueryBuilder
}
