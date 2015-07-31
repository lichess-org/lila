package lila.db
package api

import play.api.libs.json._
import reactivemongo.api.SortOrder
import reactivemongo.bson._

object $sort {

  def asc: SortOrder = SortOrder.Ascending
  def desc: SortOrder = SortOrder.Descending

  def asc(field: String): (String, SortOrder) = field -> asc
  def desc(field: String): (String, SortOrder) = field -> desc

  val ascId = asc("_id")
  val descId = desc("_id")

  val naturalAsc = asc("$natural")
  val naturalDesc = desc("$natural")
  val naturalOrder = naturalDesc

  val createdAsc = asc("createdAt")
  val createdDesc = desc("createdAt")
  val updatedDesc = desc("updatedAt")
}
