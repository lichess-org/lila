package lila.lobby

import tube.messageTube
import lila.db.Implicits._
import lila.db.api._

import play.api.libs.json._

import reactivemongo.api._

import scala.util.matching.Regex

object MessageRepo {

  private val max = 30

  def recent: Fu[List[Message]] = $find recent max
  
  def all: Fu[List[Message]] = $find.all

  def nonEmpty: Fu[List[Message]] =
    $find.all map (_ filterNot (_.isEmpty))

  def censorUsername(userId: String): Funit =
    $update(Json.obj("userId" -> userId), $set("text" -> ""), multi = true)

  def removeRegex(regex: Regex): Funit =
    $update(
      Json.obj("text" -> $regex(regex.toString)),
      $set("text" -> ""),
      multi = true)
}
