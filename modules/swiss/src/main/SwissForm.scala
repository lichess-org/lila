package lila.swiss

import chess.Clock.{ Config as ClockConfig, IncrementSeconds, LimitSeconds }
import chess.{ Speed, Rated }
import chess.format.Fen
import chess.variant.Variant
import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{ *, given }

final class SwissForm(using mode: play.api.Mode):

  import SwissForm.*

  def form(user: User, minRounds: Int = 3) =
    Form(
      mapping(
        "name" -> optional(eventName(2, 30, user.isVerifiedOrAdmin)),
        "clock" -> mapping(
          "limit" -> number.into[LimitSeconds].verifying(clockLimits.contains),
          "increment" -> number(min = 0, max = 120).into[IncrementSeconds]
        )(ClockConfig.apply)(unapply)
          .verifying("Invalid clock", _.estimateTotalSeconds > 0),
        "startsAt" -> optional(inTheFuture(ISOInstantOrTimestamp.mapping)),
        "variant" -> optional(typeIn(Variant.list.all.map(_.key).toSet)),
        "rated" -> optional(boolean.into[Rated]),
        "nbRounds" -> number(min = minRounds, max = 100),
        "description" -> optional(cleanNonEmptyText),
        "position" -> optional(lila.common.Form.fen.playableStrict),
        "chatFor" -> optional(numberIn(chatForChoices._1F)),
        "roundInterval" -> optional(numberIn(roundIntervals)),
        "password" -> optional(cleanNonEmptyText),
        "conditions" -> SwissCondition.form.all,
        "forbiddenPairings" -> optional(
          cleanNonEmptyText.verifying(
            s"Maximum forbidden pairings: ${Swiss.maxForbiddenPairings}",
            str => str.linesIterator.size <= Swiss.maxForbiddenPairings
          )
        ),
        "manualPairings" -> optional(
          cleanNonEmptyText
            .verifying(
              s"Maximum manual pairings: ${Swiss.maxForbiddenPairings}",
              str => str.linesIterator.size <= Swiss.maxForbiddenPairings
            )
            .verifying(
              "Invalid pairings, maybe a player is paired twice?",
              SwissManualPairing.validate
            )
        )
      )(SwissData.apply)(unapply)
        .verifying("15s and 0+1 variant games cannot be rated", _.validRatedVariant)
    )

  def create(user: User) =
    form(user).fill(
      SwissData(
        name = none,
        clock = ClockConfig(LimitSeconds(180), IncrementSeconds(0)),
        startsAt = Some(nowInstant.plusSeconds {
          if mode.isProd then 60 * 10 else 20
        }),
        variant = Variant.default.key.some,
        rated = Rated.Yes.some,
        nbRounds = 7,
        description = none,
        position = none,
        chatFor = Swiss.ChatFor.default.some,
        roundInterval = Swiss.RoundInterval.auto.some,
        password = None,
        conditions = SwissCondition.All.empty,
        forbiddenPairings = none,
        manualPairings = none
      )
    )

  def edit(user: User, s: Swiss) =
    form(user, s.round.value).fill(
      SwissData(
        name = s.name.some,
        clock = s.clock,
        startsAt = s.startsAt.some,
        variant = s.variant.key.some,
        rated = s.settings.rated.some,
        nbRounds = s.settings.nbRounds,
        description = s.settings.description,
        position = s.settings.position,
        chatFor = s.settings.chatFor.some,
        roundInterval = s.settings.roundInterval.toSeconds.toInt.some,
        password = s.settings.password,
        conditions = s.settings.conditions,
        forbiddenPairings = s.settings.forbiddenPairings.nonEmptyOption,
        manualPairings = s.settings.manualPairings.nonEmptyOption
      )
    )

  def nextRound = Form:
    single:
      "date" -> inTheFuture(ISOInstantOrTimestamp.mapping)

object SwissForm:

  val clockLimits = LimitSeconds.from(Seq(0, 15, 30, 45, 60, 90) ++ {
    (120 to 480 by 60) ++ (600 to 2700 by 300) ++ (3000 to 10800 by 600)
  })

  val clockLimitChoices = options(
    LimitSeconds.raw(clockLimits),
    l =>
      s"${chess.Clock.Config(LimitSeconds(l), IncrementSeconds(0)).limitString}${
          if l <= 1 then " minute" else " minutes"
        }"
  )

  val roundIntervals: Seq[Int] =
    Seq(
      Swiss.RoundInterval.auto,
      5,
      10,
      20,
      30,
      45,
      60,
      120,
      180,
      300,
      600,
      900,
      1200,
      1800,
      2700,
      3600,
      24 * 3600,
      2 * 24 * 3600,
      7 * 24 * 3600,
      Swiss.RoundInterval.manual
    )

  val roundIntervalChoices = options(
    roundIntervals,
    s =>
      if s == Swiss.RoundInterval.auto then s"Automatic"
      else if s == Swiss.RoundInterval.manual then s"Manually schedule each round"
      else if s < 60 then s"$s seconds"
      else if s < 3600 then s"${s / 60} minute(s)"
      else if s < 24 * 3600 then s"${s / 3600} hour(s)"
      else s"${s / 24 / 3600} days(s)"
  )

  val chatForChoices = List(
    Swiss.ChatFor.NONE -> "No chat",
    Swiss.ChatFor.LEADERS -> "Team leaders only",
    Swiss.ChatFor.MEMBERS -> "Team members only",
    Swiss.ChatFor.ALL -> "All Lichess players"
  )

  case class SwissData(
      name: Option[String],
      clock: ClockConfig,
      startsAt: Option[Instant],
      variant: Option[Variant.LilaKey],
      rated: Option[Rated],
      nbRounds: Int,
      description: Option[String],
      position: Option[Fen.Full],
      chatFor: Option[Int],
      roundInterval: Option[Int],
      password: Option[String],
      conditions: SwissCondition.All,
      forbiddenPairings: Option[String],
      manualPairings: Option[String]
  ):
    def realVariant = Variant.orDefault(variant)
    def realStartsAt = startsAt | nowInstant.plusMinutes(10)
    def realChatFor = chatFor | Swiss.ChatFor.default
    def realRoundInterval =
      (roundInterval | Swiss.RoundInterval.auto) match
        case Swiss.RoundInterval.auto => autoInterval(clock)
        case i => i.seconds
    def realPosition = position.ifTrue(realVariant.standard)

    def isRated = rated.forall(_.yes)
    def validRatedVariant = !isRated || lila.core.game.allowRated(realVariant, clock.some)

  def autoInterval(clock: ClockConfig) = {
    import Speed.*
    Speed(clock) match
      case UltraBullet => 5
      case Bullet => 10
      case Blitz if clock.estimateTotalSeconds < 300 => 20
      case Blitz => 30
      case Rapid => 60
      case _ => 300
  }.seconds

  val joinForm = Form(single("password" -> optional(nonEmptyText)))
