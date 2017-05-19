package lila.tournament

import play.api.data._
import play.api.data.Forms._

import chess.Mode
import chess.StartingPosition
import lila.common.Form._

final class DataForm {

  import DataForm._

  lazy val create = Form(mapping(
    "clockTime" -> numberInDouble(clockTimePrivateChoices),
    "clockIncrement" -> numberIn(clockIncrementPrivateChoices),
    "minutes" -> numberIn(minutePrivateChoices),
    "waitMinutes" -> numberIn(waitMinuteChoices),
    "variant" -> number.verifying(validVariantIds contains _),
    "position" -> nonEmptyText.verifying(positions contains _),
    "mode" -> optional(number.verifying(Mode.all map (_.id) contains _)),
    "private" -> optional(text.verifying("on" == _)),
    "password" -> optional(nonEmptyText)
  )(TournamentSetup.apply)(TournamentSetup.unapply)
    .verifying("Invalid clock", _.validClock)
    .verifying("15s variant games cannot be rated", _.validRatedUltraBulletVariant)
    .verifying("Increase tournament duration, or decrease game clock", _.validTiming)) fill TournamentSetup(
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    minutes = minuteDefault,
    waitMinutes = waitMinuteDefault,
    variant = chess.variant.Standard.id,
    position = StartingPosition.initial.eco,
    `private` = None,
    password = None,
    mode = Mode.Rated.id.some
  )
}

object DataForm {

  import chess.variant._

  val clockTimes: Seq[Double] = Seq(0d, 1 / 4d, 1 / 2d, 3 / 4d, 1d, 3 / 2d) ++ (2d to 7d by 1d)
  val clockTimesPrivate: Seq[Double] = clockTimes ++ (10d to 30d by 5d) ++ (40d to 60d by 10d)
  val clockTimeDefault = 2d
  private def formatLimit(l: Double) =
    chess.Clock.Config(l * 60 toInt, 0).limitString + {
      if (l <= 1) " minute" else " minutes"
    }
  val clockTimeChoices = optionsDouble(clockTimes, formatLimit)
  val clockTimePrivateChoices = optionsDouble(clockTimesPrivate, formatLimit)

  val clockIncrements = 0 to 2 by 1
  val clockIncrementsPrivate = clockIncrements ++ (3 to 7) ++ (10 to 30 by 5) ++ (40 to 60 by 10)
  val clockIncrementDefault = 0
  val clockIncrementChoices = options(clockIncrements, "%d second{s}")
  val clockIncrementPrivateChoices = options(clockIncrementsPrivate, "%d second{s}")

  val minutes = (20 to 60 by 5) ++ (70 to 120 by 10)
  val minutesPrivate = minutes ++ (150 to 360 by 30)
  val minuteDefault = 45
  val minuteChoices = options(minutes, "%d minute{s}")
  val minutePrivateChoices = options(minutesPrivate, "%d minute{s}")

  val waitMinutes = Seq(1, 2, 3, 5, 10, 15, 20, 30, 45, 60)
  val waitMinuteChoices = options(waitMinutes, "%d minute{s}")
  val waitMinuteDefault = 5

  val positions = StartingPosition.allWithInitial.map(_.eco)
  val positionChoices = StartingPosition.allWithInitial.map { p =>
    p.eco -> p.fullName
  }
  val positionDefault = StartingPosition.initial.eco

  val validVariants = List(Standard, Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde, RacingKings, Crazyhouse)

  val validVariantIds = validVariants.map(_.id).toSet
}

private[tournament] case class TournamentSetup(
    clockTime: Double,
    clockIncrement: Int,
    minutes: Int,
    waitMinutes: Int,
    variant: Int,
    position: String,
    mode: Option[Int],
    `private`: Option[String],
    password: Option[String]
) {

  def validClock = (clockTime + clockIncrement) > 0

  def validTiming = (minutes * 60) >= (3 * estimatedGameDuration)

  def realMode = mode.fold(Mode.default)(Mode.orDefault)

  def realVariant = chess.variant.Variant orDefault variant

  def clockConfig = chess.Clock.Config((clockTime * 60).toInt, clockIncrement)

  def validRatedUltraBulletVariant =
    realMode == Mode.Casual ||
      lila.game.Game.allowRated(realVariant, clockConfig)

  def isPrivate = `private`.isDefined

  private def estimatedGameDuration = 60 * clockTime + 30 * clockIncrement
}
