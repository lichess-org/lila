package lila.tournament

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints

import chess.Mode
import chess.StartingPosition
import lila.common.Form._
import lila.common.Form.ISODateTime._
import lila.user.User

final class DataForm {

  import DataForm._
  import UTCDate._

  def apply(user: User) = create fill TournamentSetup(
    name = canPickName(user) option user.titleUsername,
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    minutes = minuteDefault,
    waitMinutes = waitMinuteDefault.some,
    startDate = none,
    variant = chess.variant.Standard.key.some,
    position = StartingPosition.initial.fen.some,
    password = None,
    mode = none,
    rated = true.some,
    conditionsOption = Condition.DataForm.AllSetup.default.some,
    berserkable = true.some
  )

  private val nameType = text.verifying(
    Constraints minLength 2,
    Constraints maxLength 30,
    Constraints.pattern(
      regex = """[\p{L}\p{N}-\s:,;]+""".r,
      error = "error.unknown"
    )
  )

  private lazy val create = Form(mapping(
    "name" -> optional(nameType),
    "clockTime" -> numberInDouble(clockTimeChoices),
    "clockIncrement" -> numberIn(clockIncrementChoices),
    "minutes" -> numberIn(minuteChoices),
    "waitMinutes" -> optional(numberIn(waitMinuteChoices)),
    "startDate" -> optional(inTheFuture(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp)),
    "variant" -> optional(text.verifying(v => guessVariant(v).isDefined)),
    "position" -> optional(nonEmptyText),
    "mode" -> optional(number.verifying(Mode.all map (_.id) contains _)), // deprecated, use rated
    "rated" -> optional(boolean),
    "password" -> optional(nonEmptyText),
    "conditions" -> optional(Condition.DataForm.all),
    "berserkable" -> optional(boolean)
  )(TournamentSetup.apply)(TournamentSetup.unapply)
    .verifying("Invalid clock", _.validClock)
    .verifying("15s variant games cannot be rated", _.validRatedUltraBulletVariant)
    .verifying("Increase tournament duration, or decrease game clock", _.sufficientDuration)
    .verifying("Reduce tournament duration, or increase game clock", _.excessiveDuration))
}

object DataForm {

  def canPickName(u: User) = {
    u.count.game >= 10 && u.createdSinceDays(3) && !u.troll
  } || u.hasTitle

  import chess.variant._

  val clockTimes: Seq[Double] = Seq(0d, 1 / 4d, 1 / 2d, 3 / 4d, 1d, 3 / 2d) ++ (2d to 7d by 1d) ++ (10d to 30d by 5d) ++ (40d to 60d by 10d)
  val clockTimeDefault = 2d
  private def formatLimit(l: Double) =
    chess.Clock.Config(l * 60 toInt, 0).limitString + {
      if (l <= 1) " minute" else " minutes"
    }
  val clockTimeChoices = optionsDouble(clockTimes, formatLimit)

  val clockIncrements = (0 to 2 by 1) ++ (3 to 7) ++ (10 to 30 by 5) ++ (40 to 60 by 10)
  val clockIncrementDefault = 0
  val clockIncrementChoices = options(clockIncrements, "%d second{s}")

  val minutes = (20 to 60 by 5) ++ (70 to 120 by 10) ++ (150 to 360 by 30)
  val minuteDefault = 45
  val minuteChoices = options(minutes, "%d minute{s}")

  val waitMinutes = Seq(1, 2, 3, 5, 10, 15, 20, 30, 45, 60)
  val waitMinuteChoices = options(waitMinutes, "%d minute{s}")
  val waitMinuteDefault = 5

  val positions = StartingPosition.allWithInitial.map(_.fen)
  val positionChoices = StartingPosition.allWithInitial.map { p =>
    p.fen -> p.fullName
  }
  val positionDefault = StartingPosition.initial.fen

  val validVariants = List(Standard, Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde, RacingKings, Crazyhouse)

  def guessVariant(from: String): Option[Variant] = validVariants.find { v =>
    v.key == from || parseIntOption(from).exists(v.id ==)
  }

  def startingPosition(fen: String, variant: Variant): StartingPosition =
    Thematic.byFen(fen).ifTrue(variant.standard) | StartingPosition.initial
}

private[tournament] case class TournamentSetup(
    name: Option[String],
    clockTime: Double,
    clockIncrement: Int,
    minutes: Int,
    waitMinutes: Option[Int],
    startDate: Option[DateTime],
    variant: Option[String],
    position: Option[String],
    mode: Option[Int], // deprecated, use rated
    rated: Option[Boolean],
    password: Option[String],
    conditionsOption: Option[Condition.DataForm.AllSetup],
    berserkable: Option[Boolean]
) {

  def conditions = conditionsOption | Condition.DataForm.AllSetup.default

  def validClock = (clockTime + clockIncrement) > 0

  def realMode = Mode(rated.orElse(mode.map(Mode.Rated.id ==)) | true)

  def realVariant = variant.flatMap(DataForm.guessVariant) | chess.variant.Standard

  def clockConfig = chess.Clock.Config((clockTime * 60).toInt, clockIncrement)

  def validRatedUltraBulletVariant =
    realMode == Mode.Casual ||
      lila.game.Game.allowRated(realVariant, clockConfig.some)

  def sufficientDuration = estimateNumberOfGamesOneCanPlay >= 3
  def excessiveDuration = estimateNumberOfGamesOneCanPlay <= 70

  private def estimateNumberOfGamesOneCanPlay: Double = (minutes * 60) / estimatedGameSeconds

  // There are 2 players, and they don't always use all their time (0.8)
  // add 15 seconds for pairing delay
  private def estimatedGameSeconds: Double = {
    (60 * clockTime + 30 * clockIncrement) * 2 * 0.8
  } + 15
}
