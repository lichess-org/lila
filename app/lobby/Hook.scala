package lila
package lobby

import chess.{ Variant, Mode }
import elo.EloRange

import com.novus.salat.annotations.Key
import com.mongodb.DBRef

case class Hook(
    @Key("_id") id: String,
    ownerId: String,
    variant: Int,
    hasClock: Boolean,
    time: Option[Int],
    increment: Option[Int],
    mode: Int,
    color: String,
    username: String,
    elo: Option[Int],
    `match`: Boolean,
    eloRange: Option[String],
    engine: Boolean,
    game: Option[DBRef]) extends EloRange {

  def gameId: Option[String] = game map (_.getId.toString)

  def realVariant = Variant orDefault variant 

  def realMode = Mode orDefault mode

  def render = Map(
    "id" -> id,
    "username" -> username,
    "elo" -> elo,
    "variant" -> realVariant.toString,
    "mode" -> realMode.toString,
    "color" -> color,
    "clock" -> clockOrUnlimited,
    "emin" -> eloMin,
    "emax" -> eloMax
  ) +? (engine, "engine" -> true)

  def clockOrUnlimited = ((time filter (_ ⇒ hasClock)) |@| increment apply renderClock _) | "Unlimited"

  def renderClock(time: Int, inc: Int) = "%d + %d".format(time, inc)
}
