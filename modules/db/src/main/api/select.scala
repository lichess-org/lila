package lila.db
package api

import Implicits._

import play.api.libs.json._
import Json.JsValueWrapper

import play.modules.reactivemongo.Implicits._
import play.api.libs.concurrent.Execution.Implicits._

import reactivemongo.api._
import reactivemongo.bson._
import reactivemongo.core.commands._

import org.joda.time.DateTime

object $select { 

  def all = Json.obj()

  def apply[A: Writes](id: A): JsObject = byId(id)

  def byId[A: Writes](id: A) = Json.obj("_id" -> id)

  def byIds[A: Writes](ids: Seq[A]) = Json.obj("_id" -> $in(ids))
}
