package lila.tournament

import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import chess.{ Mode, Variant }
import lila.common.Form._

final class DataForm(isDev: Boolean) {

  val clockTimes = 0 to 7 by 1
  val clockTimesPrivate = clockTimes ++ (10 to 30 by 5) ++ (40 to 60 by 10)
  val clockTimeDefault = 2
  val clockTimeChoices = options(clockTimes, "%d minute{s}")
  val clockTimePrivateChoices = options(clockTimesPrivate, "%d minute{s}")

  val clockIncrements = 0 to 2 by 1
  val clockIncrementsPrivate = clockIncrements ++ (3 to 7) ++ (10 to 30 by 5) ++ (40 to 60 by 10)
  val clockIncrementDefault = 0
  val clockIncrementChoices = options(clockIncrements, "%d second{s}")
  val clockIncrementPrivateChoices = options(clockIncrementsPrivate, "%d second{s}")

  private val baseMinutes = (20 to 60 by 5) ++ (70 to 120 by 10)

  val minutes = isDev.fold((1 to 9) ++ baseMinutes, baseMinutes)
  val minutesPrivate = minutes ++ (150 to 360 by 30)
  val minuteDefault = 40
  val minuteChoices = options(minutes, "%d minute{s}")
  val minutePrivateChoices = options(minutesPrivate, "%d minute{s}")

  val minPlayers = isDev.fold(
    (2 to 9) ++ (10 to 30 by 5),
    (Tournament.minPlayers to 9) ++ (10 to 30 by 5)
  )
  val minPlayerDefault = 8
  val minPlayerChoices = options(minPlayers, "%d player{s}")

  lazy val create = Form(mapping(
    "clockTime" -> numberIn(clockTimePrivateChoices),
    "clockIncrement" -> numberIn(clockIncrementPrivateChoices),
    "minutes" -> numberIn(minutePrivateChoices),
    "minPlayers" -> numberIn(minPlayerChoices),
    "system" -> number.verifying(Set(System.Arena.id, System.Swiss.id) contains _),
    "variant" -> number.verifying(Set(Variant.Standard.id, Variant.Chess960.id, Variant.KingOfTheHill.id,
      Variant.ThreeCheck.id, Variant.SuicideChess.id) contains _),
    "mode" -> optional(number.verifying(Mode.all map (_.id) contains _)),
    "password" -> optional(nonEmptyText)
  )(TournamentSetup.apply)(TournamentSetup.unapply)
    .verifying("Invalid clock", _.validClock)
    .verifying("Increase tournament duration, or decrease game clock", _.validTiming)
  ) fill TournamentSetup(
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    minutes = minuteDefault,
    minPlayers = minPlayerDefault,
    system = System.default.id,
    variant = Variant.Standard.id,
    password = none,
    mode = Mode.Casual.id.some)

  lazy val joinPassword = Form(single(
    "password" -> nonEmptyText
  ))
}

private[tournament] case class TournamentSetup(
    clockTime: Int,
    clockIncrement: Int,
    minutes: Int,
    minPlayers: Int,
    system: Int,
    variant: Int,
    mode: Option[Int],
    password: Option[String]) {

  def validClock = (clockTime + clockIncrement) > 0

  def validTiming = (minutes * 60) >= (3 * estimatedGameDuration)

  private def estimatedGameDuration = 60 * clockTime + 30 * clockIncrement
}
