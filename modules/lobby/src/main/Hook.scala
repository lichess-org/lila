package lila.lobby

import chess.{ Variant, Mode, Clock }
import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.libs.json._

import lila.common.EloRange
import lila.user.User

case class Hook(
    id: String,
    uid: String, // owner socket uid
    sid: Option[String], // owner cookie (used to prevent multiple hooks)
    variant: Int,
    hasClock: Boolean,
    time: Option[Int],
    increment: Option[Int],
    mode: Int,
    allowAnon: Boolean,
    color: String,
    user: Option[User],
    eloRange: String,
    gameId: Option[String] = None,
    createdAt: DateTime) {

  def open = gameId.isEmpty
  def closed = !open

  def realColor = Color orDefault color

  def realVariant = Variant orDefault variant

  def realMode = Mode orDefault mode

  def memberOnly = !allowAnon

  def compatibleWith(h: Hook) =
    compatibilityProperties == h.compatibilityProperties &&
    (realColor compatibleWith h.realColor) && 
    (memberOnly || h.memberOnly).fold(isMember && h.isMember, true)

  private def compatibilityProperties = (variant, time, increment, mode)

  lazy val realEloRange: Option[EloRange] = EloRange noneIfDefault eloRange

  def userId = user map (_.id)
  def isMember = user.nonEmpty
  def username = user.fold(User.anonymous)(_.username)
  def elo = user map (_.elo)
  def engine = user ?? (_.engine)

  def render: JsObject = Json.obj(
    "id" -> id,
    "uid" -> uid,
    "username" -> username,
    "elo" -> elo,
    "variant" -> realVariant.toString,
    "mode" -> realMode.toString,
    "allowAnon" -> allowAnon,
    "clock" -> clockOption.map(c ⇒ renderClock(c.limit, c.increment)),
    "time" -> clockOption.map(_.estimateTotalTime),
    "speed" -> chess.Speed(clockOption).id,
    "emin" -> realEloRange.map(_.min),
    "emax" -> realEloRange.map(_.max),
    "color" -> chess.Color(color).??(_.name),
    "engine" -> engine)

  private def clockOption = (time filter (_ ⇒ hasClock)) |@| increment apply Clock.apply

  private def renderClock(time: Int, inc: Int) = "%d + %d".format(time / 60, inc)
}

object Hook {

  val idSize = 8

  def make(
    uid: String,
    variant: Variant,
    clock: Option[Clock],
    mode: Mode,
    allowAnon: Boolean,
    color: String,
    user: Option[User],
    sid: Option[String],
    eloRange: EloRange): Hook = new Hook(
    id = Random nextString idSize,
    uid = uid,
    variant = variant.id,
    hasClock = clock.isDefined,
    time = clock map (_.limit),
    increment = clock map (_.increment),
    mode = mode.id,
    allowAnon = allowAnon || user.isEmpty,
    color = color,
    user = user,
    sid = sid,
    eloRange = eloRange.toString,
    createdAt = DateTime.now)
}
