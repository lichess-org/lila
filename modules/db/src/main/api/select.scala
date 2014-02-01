package lila.db
package api

import play.api.libs.json._

object $select {

  def all = Json.obj()

  def apply[A: Writes](id: A): JsObject = byId(id)

  def byId[A: Writes](id: A) = Json.obj("_id" -> id)

  def byIds[A: Writes](ids: Iterable[A]) = Json.obj("_id" -> $in(ids))
}
