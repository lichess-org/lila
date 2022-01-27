package lila.lobby

import chess.{ Clock, Mode, Speed }
import org.joda.time.DateTime
import play.api.i18n.Lang
import play.api.libs.json._

import lila.game.PerfPicker
import lila.rating.RatingRange
import lila.socket.Socket.Sri
import lila.user.User

// realtime chess, volatile
case class Hook(
    id: String,
    sri: Sri,            // owner socket sri
    sid: Option[String], // owner cookie (used to prevent multiple hooks)
    variant: Int,
    clock: Clock.Config,
    mode: Int,
    color: String,
    user: Option[LobbyUser],
    ratingRange: String,
    createdAt: DateTime,
    boardApi: Boolean
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

  private def ratingRangeCompatibleWith(h: Hook) =
    realRatingRange.fold(true) { range =>
      h.rating ?? range.contains
    }

  lazy val realRatingRange: Option[RatingRange] = isAuth ?? {
    RatingRange noneIfDefault ratingRange
  }

  def userId   = user.map(_.id)
  def username = user.fold(User.anonymous)(_.username)
  def lame     = user ?? (_.lame)

  lazy val perfType = PerfPicker.perfType(speed, realVariant, none)

  lazy val perf: Option[LobbyPerf] = for { u <- user; pt <- perfType } yield u perfAt pt
  def rating: Option[Int]          = perf.map(_.rating)

  def render: JsObject =
    Json
      .obj(
        "id"    -> id,
        "sri"   -> sri,
        "clock" -> clock.show,
        "t"     -> clock.estimateTotalSeconds,
        "s"     -> speed.id,
        "i"     -> (if (clock.incrementSeconds > 0) 1 else 0)
      )
      .add("prov" -> perf.map(_.provisional).filter(identity))
      .add("u" -> user.map(_.username))
      .add("rating" -> rating)
      .add("variant" -> realVariant.exotic.option(realVariant.key))
      .add("ra" -> realMode.rated.option(1))
      .add("c" -> chess.Color.fromName(color).map(_.name))
      .add("perf" -> perfType.map(_.key))

  def randomColor = color == "random"

  lazy val compatibleWithPools =
    realMode.rated && realVariant.standard && randomColor &&
      lila.pool.PoolList.clockStringSet.contains(clock.show)

  def compatibleWithPool(poolClock: chess.Clock.Config) =
    compatibleWithPools && clock == poolClock

  def toPool =
    lila.pool.HookThieve.PoolHook(
      hookId = id,
      member = lila.pool.PoolMember(
        userId = user.??(_.id),
        sri = sri,
        rating = rating | lila.rating.Glicko.default.intRating,
        ratingRange = realRatingRange,
        lame = user.??(_.lame),
        blocking = lila.pool.PoolMember.BlockedUsers(user.??(_.blocking)),
        rageSitCounter = 0
      )
    )

  private lazy val speed = Speed(clock)
}

object Hook {

  val idSize = 8

  def make(
      sri: Sri,
      variant: chess.variant.Variant,
      clock: Clock.Config,
      mode: Mode,
      color: String,
      user: Option[User],
      sid: Option[String],
      ratingRange: RatingRange,
      blocking: Set[String],
      boardApi: Boolean = false
  ): Hook =
    new Hook(
      id = lila.common.ThreadLocalRandom nextString idSize,
      sri = sri,
      variant = variant.id,
      clock = clock,
      mode = mode.id,
      color = color,
      user = user map { LobbyUser.make(_, blocking) },
      sid = sid,
      ratingRange = ratingRange.toString,
      createdAt = DateTime.now,
      boardApi = boardApi
    )
}
