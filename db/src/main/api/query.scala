package lila.db
package api

import Types._

import reactivemongo.bson._
import play.modules.reactivemongo.Implicits._
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

object query extends query
trait query {

  def all(implicit inColl: InColl) = builder

  def apply(q: JsObject)(implicit inColl: InColl) = builder query q

  def byId[A: Writes](id: A)(implicit inColl: InColl) = apply(select byId id)

  def byIds[A: Writes](ids: Seq[A])(implicit inColl: InColl) = apply(select byIds ids)

  def builder(implicit inColl: InColl) = inColl.coll.genericQueryBuilder
}
