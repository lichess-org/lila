package lila.lobby

import chess.{ Variant, Mode, Clock }
import lila.common.EloRange
import lila.user.User

import ornicar.scalalib.Random
import play.api.libs.json._

case class Hook(
    id: String,
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

  def render: JsObject = Json.obj(
    "id" -> id,
    "username" -> username,
    "elo" -> elo,
    "variant" -> realVariant.toString,
    "mode" -> realMode.toString,
    "color" -> color,
    "clock" -> clockOrUnlimited,
    "speed" -> chess.Speed(clockOption).id,
    "emin" -> realEloRange.map(_.min),
    "emax" -> realEloRange.map(_.max),
    "engine" -> engine)

  def clockOrUnlimited = clockOption.fold("Unlimited")(c ⇒ renderClock(c.limit, c.increment))

  private def clockOption = (time filter (_ ⇒ hasClock)) |@| increment apply Clock.apply

  private def renderClock(time: Int, inc: Int) = "%d + %d".format(time / 60, inc)
}

object Hook {

  val idSize = 8
  val ownerIdSize = 12

  def make(
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
      userId = user map (_.id),
      username = user.fold(User.anonymous)(_.username),
      elo = user map (_.elo),
      eloRange = eloRange.toString,
      engine = user.zmap(_.engine))
  }

  import lila.db.Tube
  import Tube.Helpers._

  private[lobby] lazy val tube = Tube[Hook](
    reader = (__.json update merge(defaults)) andThen Json.reads[Hook],
    writer = Json.writes[Hook])

  private def defaults = Json.obj(
    "match" -> false,
    "gameId" -> none[String])

  private def generateId =
    Random nextString idSize

  private def generateOwnerId =
    Random nextString (ownerIdSize - idSize)
}
