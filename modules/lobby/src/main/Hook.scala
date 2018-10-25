package lila.lobby

import chess.{ Mode, Clock, Speed }
import org.joda.time.DateTime
import ornicar.scalalib.Random
import play.api.libs.json._

import lila.game.PerfPicker
import lila.rating.RatingRange
import lila.user.User
import lila.socket.Socket.Uid

// realtime chess, volatile
case class Hook(
    id: String,
    uid: Uid, // owner socket uid
    sid: Option[String], // owner cookie (used to prevent multiple hooks)
    variant: Int,
    clock: Clock.Config,
    mode: Int,
    color: String,
    user: Option[LobbyUser],
    ratingRange: String,
    createdAt: DateTime
) {

  val realColor = Color orDefault color

  val realVariant = chess.variant.Variant orDefault variant

  val realMode = Mode orDefault mode

  val isAuth = user.nonEmpty

  def compatibleWith(h: Hook) =
    isAuth == h.isAuth &&
      mode == h.mode &&
      variant == h.variant &&
      clock == h.clock &&
      (realColor compatibleWith h.realColor) &&
      ratingRangeCompatibleWith(h) && h.ratingRangeCompatibleWith(this) &&
      (userId.isEmpty || userId != h.userId)

  private def ratingRangeCompatibleWith(h: Hook) = realRatingRange.fold(true) {
    range => h.rating ?? range.contains
  }

  lazy val realRatingRange: Option[RatingRange] = isAuth ?? {
    RatingRange noneIfDefault ratingRange
  }

  def userId = user.map(_.id)
  def username = user.fold(User.anonymous)(_.username)
  def lame = user ?? (_.lame)

  lazy val perfType = PerfPicker.perfType(speed, realVariant, none)

  lazy val perf: Option[LobbyPerf] = for { u <- user; pt <- perfType } yield u perfAt pt
  def rating: Option[Int] = perf.map(_.rating)

  lazy val render: JsObject = Json.obj(
    "id" -> id,
    "uid" -> uid,
    "clock" -> clock.show,
    "t" -> clock.estimateTotalSeconds,
    "s" -> speed.id
  ).add("prov" -> perf.map(_.provisional).filter(identity))
    .add("u" -> user.map(_.username))
    .add("rating" -> rating)
    .add("variant" -> realVariant.exotic.option(realVariant.key))
    .add("ra" -> realMode.rated.option(1))
    .add("c" -> chess.Color(color).map(_.name))
    .add("perf" -> perfType.map(_.name))

  def randomColor = color == "random"

  lazy val compatibleWithPools =
    realMode.rated && realVariant.standard && randomColor &&
      lila.pool.PoolList.clockStringSet.contains(clock.show)

  def compatibleWithPool(poolClock: chess.Clock.Config) =
    compatibleWithPools && clock == poolClock

  def likePoolFiveO = compatibleWithPools && clock.show == "5+0"

  def toPool = lila.pool.HookThieve.PoolHook(
    hookId = id,
    member = lila.pool.PoolMember(
      userId = user.??(_.id),
      uid = uid,
      rating = rating | lila.rating.Glicko.defaultIntRating,
      ratingRange = realRatingRange,
      lame = user.??(_.lame),
      blocking = lila.pool.PoolMember.BlockedUsers(user.??(_.blocking)),
      since = createdAt
    )
  )

  private lazy val speed = Speed(clock)
}

object Hook {

  val idSize = 8

  def make(
    uid: Uid,
    variant: chess.variant.Variant,
    clock: Clock.Config,
    mode: Mode,
    color: String,
    user: Option[User],
    sid: Option[String],
    ratingRange: RatingRange,
    blocking: Set[String]
  ): Hook = new Hook(
    id = Random nextString idSize,
    uid = uid,
    variant = variant.id,
    clock = clock,
    mode = mode.id,
    color = color,
    user = user map { LobbyUser.make(_, blocking) },
    sid = sid,
    ratingRange = ratingRange.toString,
    createdAt = DateTime.now
  )
}
