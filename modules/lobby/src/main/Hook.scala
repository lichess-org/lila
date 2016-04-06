package lila.lobby

import chess.{ Mode, Clock, Speed }
import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.libs.json._

import actorApi.LobbyUser
import lila.game.PerfPicker
import lila.rating.RatingRange
import lila.user.{ User, Perfs }
import lila.common.PimpedJson._

// realtime chess, volatile
case class Hook(
    id: String,
    uid: String, // owner socket uid
    sid: Option[String], // owner cookie (used to prevent multiple hooks)
    variant: Int,
    clock: Clock,
    mode: Int,
    allowAnon: Boolean,
    color: String,
    user: Option[LobbyUser],
    ratingRange: String,
    createdAt: DateTime) {

  val realColor = Color orDefault color

  val realVariant = chess.variant.Variant orDefault variant

  val realMode = Mode orDefault mode

  def memberOnly = !allowAnon

  def compatibleWith(h: Hook) =
    compatibilityProperties == h.compatibilityProperties &&
      (realColor compatibleWith h.realColor) &&
      (memberOnly || h.memberOnly).fold(isAuth && h.isAuth, true) &&
      ratingRangeCompatibleWith(h) && h.ratingRangeCompatibleWith(this) &&
      (userId.isEmpty || userId != h.userId)

  private def ratingRangeCompatibleWith(h: Hook) = realRatingRange.fold(true) {
    range => h.rating ?? range.contains
  }

  private def compatibilityProperties = (variant, clock.limit, clock.increment, mode)

  lazy val realRatingRange: Option[RatingRange] = RatingRange noneIfDefault ratingRange

  def userId = user.map(_.id)
  def isAuth = user.nonEmpty
  def username = user.fold(User.anonymous)(_.username)
  def rating = user flatMap { u => perfType map (_.key) flatMap u.ratingMap.get }
  def engine = user ?? (_.engine)
  def booster = user ?? (_.booster)
  def lame = user ?? (_.lame)

  def render: JsObject = Json.obj(
    "id" -> id,
    "uid" -> uid,
    "u" -> user.map(_.username),
    "rating" -> rating,
    "variant" -> realVariant.exotic.option(realVariant.key),
    "ra" -> realMode.rated.option(1),
    "clock" -> clock.show,
    "t" -> clock.estimateTotalTime,
    "s" -> speed.id,
    "c" -> chess.Color(color).map(_.name),
    "perf" -> perfType.map(_.name)
  ).noNull

  lazy val perfType = PerfPicker.perfType(speed, realVariant, none)

  private lazy val speed = Speed(clock.some)
}

object Hook {

  val idSize = 8

  def make(
    uid: String,
    variant: chess.variant.Variant,
    clock: Clock,
    mode: Mode,
    allowAnon: Boolean,
    color: String,
    user: Option[User],
    sid: Option[String],
    ratingRange: RatingRange,
    blocking: Set[String]): Hook = new Hook(
    id = Random nextStringUppercase idSize,
    uid = uid,
    variant = variant.id,
    clock = clock,
    mode = mode.id,
    allowAnon = allowAnon || user.isEmpty,
    color = color,
    user = user map { LobbyUser.make(_, blocking) },
    sid = sid,
    ratingRange = ratingRange.toString,
    createdAt = DateTime.now)
}
