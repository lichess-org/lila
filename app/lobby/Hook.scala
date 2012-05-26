package lila
package lobby

import chess.{ Variant, Mode, Clock }
import setup.Color
import elo.EloRange
import user.User

import com.novus.salat.annotations.Key
import ornicar.scalalib.OrnicarRandom

case class Hook(
    @Key("_id") id: String,
    ownerId: String,
    variant: Int,
    hasClock: Boolean,
    time: Option[Int],
    increment: Option[Int],
    mode: Int,
    color: String,
    userId: Option[String],
    username: String,
    elo: Option[Int],
    eloRange: String,
    engine: Boolean,
    `match`: Boolean = false,
    gameId: Option[String] = None) {

  def realColor = Color orDefault color

  def realVariant = Variant orDefault variant

  def realMode = Mode orDefault mode

  lazy val realEloRange: Option[EloRange] = EloRange noneIfDefault eloRange

  def render = Map(
    "id" -> id,
    "username" -> username,
    "elo" -> elo,
    "variant" -> realVariant.toString,
    "mode" -> realMode.toString,
    "color" -> color,
    "clock" -> clockOrUnlimited,
    "emin" -> realEloRange.map(_.min),
    "emax" -> realEloRange.map(_.max)
  ) +? (engine, "engine" -> true)

  def clockOrUnlimited = ((time filter (_ ⇒ hasClock)) |@| increment apply renderClock _) | "Unlimited"

  def renderClock(time: Int, inc: Int) = "%d + %d".format(time, inc)
}

object Hook {

  val idSize = 8
  val ownerIdSize = 12

  def apply(
    variant: Variant,
    clock: Option[Clock],
    mode: Mode,
    color: String,
    user: Option[User],
    eloRange: EloRange): Hook = generateId |> { id ⇒
    new Hook(
      id = id,
      ownerId = id + generateOwnerId,
      variant = variant.id,
      hasClock = clock.isDefined,
      time = clock map (_.limit),
      increment = clock map (_.increment),
      mode = mode.id,
      color = color,
      userId = user map (_.idString),
      username = user.fold(_.username, User.anonymous),
      elo = user map (_.elo),
      eloRange = eloRange.toString,
      engine = user.fold(_.engine, false))
  }

  private def generateId =
    OrnicarRandom nextAsciiString idSize

  private def generateOwnerId =
    OrnicarRandom nextAsciiString (ownerIdSize - idSize)
}
