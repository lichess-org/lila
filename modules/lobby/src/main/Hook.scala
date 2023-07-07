package lila.lobby

import chess.{ Clock, Mode, Speed }
import chess.variant.Variant
import play.api.libs.json.*
import ornicar.scalalib.ThreadLocalRandom

import lila.rating.RatingRange
import lila.socket.Socket.Sri
import lila.user.User
import lila.rating.PerfType

// realtime chess, volatile
case class Hook(
    id: String,
    sri: Sri,            // owner socket sri
    sid: Option[String], // owner cookie (used to prevent multiple hooks)
    variant: Variant.Id,
    clock: Clock.Config,
    mode: Int,
    color: String,
    user: Option[LobbyUser],
    ratingRange: String,
    createdAt: Instant,
    boardApi: Boolean
):

  val realColor = Color orDefault color

  val realVariant = Variant.orDefault(variant)

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
    !isAuth || {
      h.rating so ratingRangeOrDefault.contains
    }

  private lazy val manualRatingRange = isAuth.so(RatingRange noneIfDefault ratingRange)

  private def nonWideRatingRange =
    val r = rating | lila.rating.Glicko.default.intRating
    manualRatingRange.filter:
      _ != RatingRange(r - 500, r + 500)

  lazy val ratingRangeOrDefault: RatingRange =
    nonWideRatingRange orElse
      rating.map(RatingRange.defaultFor) getOrElse
      RatingRange.default

  def userId   = user.map(_.id)
  def username = user.fold(User.anonymous)(_.username)
  def lame     = user.so(_.lame)

  lazy val perfType: PerfType = PerfType(realVariant, speed)

  lazy val perf: Option[LobbyPerf] = user.map(_.perfAt(perfType))
  def rating: Option[IntRating]    = perf.map(_.rating)

  import lila.common.Json.given
  def render: JsObject = Json
    .obj(
      "id"    -> id,
      "sri"   -> sri,
      "clock" -> clock.show,
      "perf"  -> perfType.key,
      "t"     -> clock.estimateTotalSeconds,
      "s"     -> speed.id,
      "i"     -> (if clock.incrementSeconds > 0 then 1 else 0)
    )
    .add("prov" -> perf.map(_.provisional))
    .add("u" -> user.map(_.username))
    .add("rating" -> rating)
    .add("variant" -> realVariant.exotic.option(realVariant.key))
    .add("ra" -> realMode.rated.option(1))
    .add("c" -> chess.Color.fromName(color).map(_.name))

  def randomColor = color == "random"

  lazy val compatibleWithPools =
    realMode.rated && realVariant.standard && randomColor &&
      lila.pool.PoolList.clockStringSet.contains(clock.show)

  def compatibleWithPool(poolClock: chess.Clock.Config) =
    compatibleWithPools && clock == poolClock

  def toPool = user.map: u =>
    lila.pool.HookThieve.PoolHook(
      hookId = id,
      member = lila.pool.PoolMember(
        userId = u.id,
        sri = sri,
        rating = rating | lila.rating.Glicko.default.intRating,
        ratingRange = manualRatingRange,
        lame = user.so(_.lame),
        blocking = user.so(_.blocking),
        rageSitCounter = 0
      )
    )

  private lazy val speed = Speed(clock)

object Hook:

  val idSize = 8

  def make(
      sri: Sri,
      variant: chess.variant.Variant,
      clock: Clock.Config,
      mode: Mode,
      color: String,
      user: Option[User.WithPerfs],
      sid: Option[String],
      ratingRange: RatingRange,
      blocking: lila.pool.Blocking,
      boardApi: Boolean = false
  ): Hook =
    new Hook(
      id = ThreadLocalRandom nextString idSize,
      sri = sri,
      variant = variant.id,
      clock = clock,
      mode = mode.id,
      color = color,
      user = user.map(LobbyUser.make(_, blocking)),
      sid = sid,
      ratingRange = ratingRange.toString,
      createdAt = nowInstant,
      boardApi = boardApi
    )
