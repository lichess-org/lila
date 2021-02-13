package lila.swiss

import chess.Clock.{ Config => ClockConfig }
import chess.variant.Variant
import org.joda.time.DateTime
import play.api.Mode
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints
import scala.concurrent.duration._

import lila.common.Form._

final class SwissForm(implicit mode: Mode) {

  import SwissForm._

  def form(minRounds: Int = 3) =
    Form(
      mapping(
        "name" -> optional(
          clean(text).verifying(
            Constraints minLength 2,
            Constraints maxLength 30,
            Constraints.pattern(
              regex = """[\p{L}\p{N}-\s:,;\+]+""".r,
              error = "Invalid characters"
            )
          )
        ),
        "clock" -> mapping(
          "limit"     -> number.verifying(clockLimits.contains _),
          "increment" -> number(min = 0, max = 600)
        )(ClockConfig.apply)(ClockConfig.unapply)
          .verifying("Invalid clock", _.estimateTotalSeconds > 0),
        "startsAt"      -> optional(inTheFuture(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp)),
        "variant"       -> optional(nonEmptyText.verifying(v => Variant(v).isDefined)),
        "rated"         -> optional(boolean),
        "nbRounds"      -> number(min = minRounds, max = 100),
        "description"   -> optional(clean(nonEmptyText)),
        "chatFor"       -> optional(numberIn(chatForChoices.map(_._1))),
        "roundInterval" -> optional(numberIn(roundIntervals)),
        "password"      -> optional(clean(nonEmptyText))
      )(SwissData.apply)(SwissData.unapply)
    )

  def create =
    form() fill SwissData(
      name = none,
      clock = ClockConfig(180, 0),
      startsAt = Some(DateTime.now plusSeconds {
        if (mode == Mode.Prod) 60 * 10 else 20
      }),
      variant = Variant.default.key.some,
      rated = true.some,
      nbRounds = 7,
      description = none,
      chatFor = Swiss.ChatFor.default.some,
      roundInterval = Swiss.RoundInterval.auto.some,
      password = None
    )

  def edit(s: Swiss) =
    form(s.round.value) fill SwissData(
      name = s.name.some,
      clock = s.clock,
      startsAt = s.startsAt.some,
      variant = s.variant.key.some,
      rated = s.settings.rated.some,
      nbRounds = s.settings.nbRounds,
      description = s.settings.description,
      chatFor = s.settings.chatFor.some,
      roundInterval = s.settings.roundInterval.toSeconds.toInt.some,
      password = s.settings.password
    )

  def nextRound =
    Form(
      single(
        "date" -> inTheFuture(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp)
      )
    )
}

object SwissForm {

  val clockLimits: Seq[Int] = Seq(0, 15, 30, 45, 60, 90) ++ {
    (120 to 420 by 60) ++ (600 to 1800 by 300) ++ (2400 to 10800 by 600)
  }

  val clockLimitChoices = options(
    clockLimits,
    l => s"${chess.Clock.Config(l, 0).limitString}${if (l <= 1) " minute" else " minutes"}"
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
      90,
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
      if (s == Swiss.RoundInterval.auto) s"Automatic"
      else if (s == Swiss.RoundInterval.manual) s"Manually schedule each round"
      else if (s < 60) s"$s seconds"
      else if (s < 3600) s"${s / 60} minute(s)"
      else if (s < 24 * 3600) s"${s / 3600} hour(s)"
      else s"${s / 24 / 3600} days(s)"
  )

  val chatForChoices = List(
    Swiss.ChatFor.NONE    -> "No chat",
    Swiss.ChatFor.LEADERS -> "Team leaders only",
    Swiss.ChatFor.MEMBERS -> "Team members only",
    Swiss.ChatFor.ALL     -> "All Lichess players"
  )

  case class SwissData(
      name: Option[String],
      clock: ClockConfig,
      startsAt: Option[DateTime],
      variant: Option[String],
      rated: Option[Boolean],
      nbRounds: Int,
      description: Option[String],
      chatFor: Option[Int],
      roundInterval: Option[Int],
      password: Option[String]
  ) {
    def realVariant  = variant flatMap Variant.apply getOrElse Variant.default
    def realStartsAt = startsAt | DateTime.now.plusMinutes(10)
    def realChatFor  = chatFor | Swiss.ChatFor.default
    def realRoundInterval = {
      (roundInterval | Swiss.RoundInterval.auto) match {
        case Swiss.RoundInterval.auto =>
          import chess.Speed._
          chess.Speed(clock) match {
            case UltraBullet                               => 5
            case Bullet                                    => 10
            case Blitz if clock.estimateTotalSeconds < 300 => 20
            case Blitz                                     => 30
            case Rapid                                     => 60
            case _                                         => 300
          }
        case i => i
      }
    }.seconds
  }
}
