package lila.setup

import chess.variant.Variant
import chess.{ Clock, Mode }
import chess.IntRating
import scalalib.model.Days

import lila.core.perf.UserWithPerfs
import lila.core.rating.RatingRange
import lila.lobby.{ Hook, Seek, TriColor }
import lila.rating.RatingRange.withinLimits

case class HookConfig(
    variant: chess.variant.Variant,
    timeMode: TimeMode,
    time: Double, // minutes
    increment: Clock.IncrementSeconds,
    days: Days,
    mode: Mode,
    color: TriColor,
    ratingRange: RatingRange
) extends HumanConfig:

  def withinLimits(using me: Option[Me], perf: Perf): HookConfig =
    if me.isEmpty then this
    else copy(ratingRange = ratingRange.withinLimits(perf.intRating, 500))

  def >> = (
    variant.id,
    timeMode.id,
    time,
    increment,
    days,
    mode.id.some,
    ratingRange.toString.some,
    color.name.some
  ).some

  def withTimeModeString(tc: Option[String]) =
    tc match
      case Some("realTime")       => copy(timeMode = TimeMode.RealTime)
      case Some("correspondence") => copy(timeMode = TimeMode.Correspondence)
      case Some("unlimited")      => copy(timeMode = TimeMode.Unlimited)
      case _                      => this

  def hook(
      sri: lila.core.socket.Sri,
      user: Option[UserWithPerfs],
      sid: Option[String],
      blocking: lila.core.pool.Blocking
  ): Either[Hook, Option[Seek]] =
    timeMode match
      case TimeMode.RealTime =>
        val clock = justMakeClock
        Left:
          Hook.make(
            sri = sri,
            variant = variant,
            clock = clock,
            mode = if lila.core.game.allowRated(variant, clock.some) then mode else Mode.Casual,
            color = color,
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
              user = u,
              blocking = blocking,
              ratingRange = ratingRange
            )

  def updateFrom(game: Game) =
    val h1 = copy(
      variant = game.variant,
      timeMode = TimeMode.ofGame(game),
      time = game.clock.map(_.limitInMinutes) | time,
      increment = game.clock.map(_.incrementSeconds) | increment,
      days = game.daysPerTurn | days,
      mode = game.mode
    )
    val h2 = if h1.isRatedUnlimited then h1.copy(mode = Mode.Casual) else h1
    if !h2.validClock then h2.copy(time = 1) else h2

  def withRatingRange(ratingRange: String) =
    copy(ratingRange = RatingRange.orDefault(ratingRange))
  def withRatingRange(rating: Option[IntRating], deltaMin: Option[String], deltaMax: Option[String]) =
    copy(ratingRange = lila.rating.RatingRange.orDefault(rating, deltaMin, deltaMax))

object HookConfig extends BaseHumanConfig:

  def from(
      v: Variant.Id,
      tm: Int,
      t: Double,
      i: Clock.IncrementSeconds,
      d: Days,
      m: Option[Int],
      e: Option[String],
      c: Option[String]
  ) =
    val realMode = m.fold(Mode.default)(Mode.orDefault)
    new HookConfig(
      variant = chess.variant.Variant.orDefault(v),
      timeMode = TimeMode(tm).err(s"Invalid time mode $tm"),
      time = t,
      increment = i,
      days = d,
      mode = realMode,
      color = TriColor.orDefault(c),
      ratingRange = e.fold(RatingRange.default)(RatingRange.orDefault)
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
    color = TriColor.default
  )

  import lila.db.BSON
  import lila.db.dsl.{ *, given }

  private[setup] given BSON[HookConfig] with

    def reads(r: BSON.Reader): HookConfig =
      HookConfig(
        variant = Variant.idOrDefault(r.getO[Variant.Id]("v")),
        timeMode = TimeMode.orDefault(r.int("tm")),
        time = r.double("t"),
        increment = r.get("i"),
        days = r.get("d"),
        mode = Mode.orDefault(r.int("m")),
        color = TriColor.Random,
        ratingRange = r.strO("e").flatMap(RatingRange.parse).getOrElse(RatingRange.default)
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
