package lila.db
package api

import Implicits._
import org.joda.time.DateTime
import play.api.libs.json._
import play.modules.reactivemongo.json.ImplicitBSONHandlers._
import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands._

object $select { 

  def all = Json.obj()

  def apply[A: Writes](id: A): JsObject = byId(id)

  def byId[A: Writes](id: A) = Json.obj("_id" -> id)

  def byIds[A: Writes](ids: Iterable[A]) = Json.obj("_id" -> $in(ids))
}
