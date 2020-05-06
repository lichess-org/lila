package lila.swiss

import chess.Clock.{ Config => ClockConfig }
import chess.variant.Variant
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints
import scala.concurrent.duration._

import lila.common.Form._

final class SwissForm {

  import SwissForm._

  val form = Form(
    mapping(
      "name" -> optional(
        text.verifying(
          Constraints minLength 2,
          Constraints maxLength 30,
          Constraints.pattern(
            regex = """[\p{L}\p{N}-\s:,;]+""".r,
            error = "error.unknown"
          )
        )
      ),
      "clock" -> mapping(
        "limit"     -> number.verifying(clockLimits.contains _),
        "increment" -> number(min = 0, max = 180)
      )(ClockConfig.apply)(ClockConfig.unapply)
        .verifying("Invalid clock", _.estimateTotalSeconds > 0),
      "startsAt"      -> optional(inTheFuture(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp)),
      "variant"       -> nonEmptyText.verifying(v => Variant(v).isDefined),
      "rated"         -> boolean,
      "nbRounds"      -> number(min = 3, max = 50),
      "description"   -> optional(nonEmptyText),
      "hasChat"       -> optional(boolean),
      "roundInterval" -> optional(number(min = 5, max = 3600))
    )(SwissData.apply)(SwissData.unapply)
  )

  def create =
    form fill SwissData(
      name = none,
      clock = ClockConfig(180, 0),
      startsAt = Some(DateTime.now plusMinutes 1),
      variant = Variant.default.key,
      rated = true,
      nbRounds = 10,
      description = none,
      hasChat = true.some,
      roundInterval = 60.some
    )

  def edit(s: Swiss) =
    form fill SwissData(
      name = s.name.some,
      clock = s.clock,
      startsAt = s.startsAt.some,
      variant = s.variant.key,
      rated = s.settings.rated,
      nbRounds = s.settings.nbRounds,
      description = s.settings.description,
      hasChat = s.settings.hasChat.some,
      roundInterval = s.settings.roundInterval.toSeconds.toInt.some
    )
}

object SwissForm {

  val clockLimits: Seq[Int] = Seq(0, 15, 30, 45, 60, 90) ++ {
    (120 to 420 by 60) ++ (600 to 1800 by 300) ++ (2400 to 3600 by 600)
  }

  val clockLimitChoices = options(
    clockLimits,
    l => s"${chess.Clock.Config(l, 0).limitString}${if (l <= 1) " minute" else " minutes"}"
  )

  val roundIntervals: Seq[Int] =
    Seq(5, 10, 20, 30, 45, 60, 90, 120, 180, 300, 600, 900, 1200, 1800, 2700, 3600)

  val roundIntervalChoices = options(
    roundIntervals,
    s => if (s < 60) s"$s seconds" else s"${s / 60} minute(s)"
  )

  case class SwissData(
      name: Option[String],
      clock: ClockConfig,
      startsAt: Option[DateTime],
      variant: String,
      rated: Boolean,
      nbRounds: Int,
      description: Option[String],
      hasChat: Option[Boolean],
      roundInterval: Option[Int]
  ) {
    def realVariant       = Variant orDefault variant
    def realStartsAt      = startsAt | DateTime.now.plusMinutes(10)
    def realRoundInterval = (roundInterval | 60).seconds
  }
}
