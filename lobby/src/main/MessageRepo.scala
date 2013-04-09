package lila.lobby

import tube.messageTube
import lila.db.Implicits._
import lila.db.api._

import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import reactivemongo.api._
import reactivemongo.bson._
import play.modules.reactivemongo.MongoJSONHelpers.RegEx

private[lobby] object MessageRepo {

  def nonEmpty: Fu[List[Message]] =
    $find($select.all) map (_ filterNot (_.isEmpty))

  def censorUsername(userId: String): Funit =
    $update(Json.obj("userId" -> userId), $set("text" -> ""), multi = true)

  def removeRegex(regex: scala.util.matching.Regex): Funit =
    $update(
      Json.obj("text" -> RegEx(regex.toString)),
      $set("text" -> ""),
      multi = true)
}
