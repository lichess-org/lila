package lila.timeline

import play.api.libs.json.Json

import lila.db.api._
import lila.db.Implicits._
import tube.{ entryTube, gameEntryTube }

private[timeline] final class Getter(
    gameMax: Int,
    userMax: Int) {

  def recentGames: Fu[List[GameEntry]] =
    $find[GameEntry](
      $query[GameEntry]($select.all) sort $sort.naturalOrder,
      gameMax)

  def userEntries(userId: String): Fu[List[Entry]] = 
    _userEntries(userId, userMax)

  def moreUserEntries(userId: String): Fu[List[Entry]] = 
    _userEntries(userId, 100)

  private def _userEntries(userId: String, max: Int): Fu[List[Entry]] =
    $find[Entry](
      $query[Entry](Json.obj("users" -> userId)) sort $sort.desc("date"),
      max)
}
