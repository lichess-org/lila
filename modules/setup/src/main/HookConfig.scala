package lila.setup

import chess.{ Mode, Clock }
import chess.variant.Variant

import lila.common.Days
import lila.lobby.{ Color, Hook, Seek }
import lila.rating.RatingRange
import lila.user.{ Me, User }
import lila.rating.{ Perf, PerfType }

case class HookConfig(
    variant: chess.variant.Variant,
    timeMode: TimeMode,
    time: Double,
    increment: Clock.IncrementSeconds,
    days: Days,
    mode: Mode,
    color: Color,
    ratingRange: RatingRange
) extends HumanConfig:

  def withinLimits(using me: Option[Me], perf: Perf): HookConfig =
    if me.isEmpty then this
    else copy(ratingRange = ratingRange.withinLimits(perf.intRating, 500))

  def fixColor = copy(
    color =
      if mode == Mode.Rated &&
        lila.game.Game.variantsWhereWhiteIsBetter(variant) &&
        color != Color.Random
      then Color.Random
      else color
  )

  def >> =
    (variant.id, timeMode.id, time, increment, days, mode.id.some, ratingRange.toString.some, color.name).some

  def withTimeModeString(tc: Option[String]) =
    tc match
      case Some("realTime")       => copy(timeMode = TimeMode.RealTime)
      case Some("correspondence") => copy(timeMode = TimeMode.Correspondence)
      case Some("unlimited")      => copy(timeMode = TimeMode.Unlimited)
      case _                      => this

  def hook(
      sri: lila.socket.Socket.Sri,
      user: Option[User.WithPerfs],
      sid: Option[String],
      blocking: lila.pool.Blocking
  ): Either[Hook, Option[Seek]] =
    timeMode match
      case TimeMode.RealTime =>
        val clock = justMakeClock
        Left:
          Hook.make(
            sri = sri,
            variant = variant,
            clock = clock,
            mode = if lila.game.Game.allowRated(variant, clock.some) then mode else Mode.Casual,
            color = color.name,
            user = user,
            blocking = blocking,
            sid = sid,
            ratingRange = ratingRange
          )
      case _ =>
        Right:
          user.map: u =>
            Seek.make(
              variant = variant,
              daysPerTurn = makeDaysPerTurn,
              mode = mode,
              color = color.name,
              user = u,
              blocking = blocking,
              ratingRange = ratingRange
            )

  def updateFrom(game: lila.game.Game) =
    copy(
      variant = game.variant,
      timeMode = TimeMode ofGame game,
      time = game.clock.map(_.limitInMinutes) | time,
      increment = game.clock.map(_.incrementSeconds) | increment,
      days = game.daysPerTurn | days,
      mode = game.mode
    )

  def withRatingRange(ratingRange: String) = copy(ratingRange = RatingRange orDefault ratingRange)
  def withRatingRange(rating: Option[IntRating], deltaMin: Option[String], deltaMax: Option[String]) =
    copy(ratingRange = RatingRange.orDefault(rating, deltaMin, deltaMax))

object HookConfig extends BaseHumanConfig:

  def from(
      v: Variant.Id,
      tm: Int,
      t: Double,
      i: Clock.IncrementSeconds,
      d: Days,
      m: Option[Int],
      e: Option[String],
      c: String
  ) =
    val realMode = m.fold(Mode.default)(Mode.orDefault)
    new HookConfig(
      variant = chess.variant.Variant.orDefault(v),
      timeMode = TimeMode(tm) err s"Invalid time mode $tm",
      time = t,
      increment = i,
      days = d,
      mode = realMode,
      ratingRange = e.fold(RatingRange.default)(RatingRange.orDefault),
      color = Color(c) err s"Invalid color $c"
    )

  def default(auth: Boolean): HookConfig = default.copy(mode = Mode(auth))

  private val default = HookConfig(
    variant = variantDefault,
    timeMode = TimeMode.RealTime,
    time = 5d,
    increment = Clock.IncrementSeconds(3),
    days = Days(2),
    mode = Mode.default,
    ratingRange = RatingRange.default,
    color = Color.default
  )

  import lila.db.BSON
  import lila.db.dsl.{ *, given }

  private[setup] given BSON[HookConfig] with

    def reads(r: BSON.Reader): HookConfig =
      HookConfig(
        variant = Variant idOrDefault r.getO[Variant.Id]("v"),
        timeMode = TimeMode orDefault (r int "tm"),
        time = r double "t",
        increment = r get "i",
        days = r.get("d"),
        mode = Mode orDefault (r int "m"),
        color = Color.Random,
        ratingRange = r strO "e" flatMap RatingRange.apply getOrElse RatingRange.default
      )

    def writes(w: BSON.Writer, o: HookConfig) =
      $doc(
        "v"  -> o.variant.id,
        "tm" -> o.timeMode.id,
        "t"  -> o.time,
        "i"  -> o.increment,
        "d"  -> o.days,
        "m"  -> o.mode.id,
        "e"  -> o.ratingRange.toString
      )
