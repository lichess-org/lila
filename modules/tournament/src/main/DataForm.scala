package lidraughts.tournament

import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation
import play.api.data.validation.{ Constraint, Constraints }

import draughts.Mode
import draughts.StartingPosition
import draughts.variant.{ Variant, Standard, Russian }
import lidraughts.common.Form._
import lidraughts.common.Form.ISODateTime._
import lidraughts.user.User

final class DataForm {

  import DataForm._
  import UTCDate._

  def create(user: User) = form(user) fill TournamentSetup(
    name = canPickName(user) option user.titleUsername,
    clockTime = clockTimeDefault,
    clockIncrement = clockIncrementDefault,
    minutes = minuteDefault,
    waitMinutes = waitMinuteDefault.some,
    startDate = none,
    variant = draughts.variant.Standard.id.toString.some,
    positionStandard = Standard.initialFen.some,
    positionRussian = Russian.initialFen.some,
    password = None,
    mode = none,
    rated = true.some,
    conditions = Condition.DataForm.AllSetup.default,
    berserkable = true.some,
    description = none
  )

  def edit(user: User, tour: Tournament) = form(user) fill TournamentSetup(
    name = tour.name.some,
    clockTime = tour.clock.limitInMinutes,
    clockIncrement = tour.clock.incrementSeconds,
    minutes = tour.minutes,
    waitMinutes = none,
    startDate = tour.startsAt.some,
    variant = tour.variant.id.toString.some,
    positionStandard = if (tour.variant.standard) tour.openingTable.fold(tour.position.fen)(_.key).some else Standard.initialFen.some,
    positionRussian = if (tour.variant.russian) tour.openingTable.fold(tour.position.fen)(_.key).some else Russian.initialFen.some,
    mode = none,
    rated = tour.mode.rated.some,
    password = tour.password,
    conditions = Condition.DataForm.AllSetup(tour.conditions),
    berserkable = tour.berserkable.some,
    description = tour.description
  )

  private val nameType = text.verifying(
    Constraints minLength 2,
    Constraints maxLength 30,
    Constraints.pattern(
      regex = """[\p{L}\p{N}-\s:,;]+""".r,
      error = "error.unknown"
    ),
    Constraint[String] { (t: String) =>
      if (t.toLowerCase contains "lidraughts") validation.Invalid(validation.ValidationError("Must not contain \"lidraughts\""))
      else validation.Valid
    }
  )

  private def form(user: User) = Form(mapping(
    "name" -> optional(nameType),
    "clockTime" -> numberInDouble(clockTimeChoices),
    "clockIncrement" -> numberIn(clockIncrementChoices),
    "minutes" -> {
      if (lidraughts.security.Granter(_.ManageTournament)(user)) number
      else numberIn(minuteChoices)
    },
    "waitMinutes" -> optional(numberIn(waitMinuteChoices)),
    "startDate" -> optional(inTheFuture(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp)),
    "variant" -> optional(text.verifying(v => guessVariant(v).isDefined)),
    "position_standard" -> optional(nonEmptyText),
    "position_russian" -> optional(nonEmptyText),
    "mode" -> optional(number.verifying(Mode.all map (_.id) contains _)), // deprecated, use rated
    "rated" -> optional(boolean),
    "password" -> optional(nonEmptyText),
    "conditions" -> Condition.DataForm.all,
    "berserkable" -> optional(boolean),
    "description" -> optional(nonEmptyText(maxLength = 600))
  )(TournamentSetup.apply)(TournamentSetup.unapply)
    .verifying("Invalid clock", _.validClock)
    .verifying("15s variant games cannot be rated", _.validRatedUltraBulletVariant)
    .verifying("Increase tournament duration, or decrease game clock", _.sufficientDuration)
    .verifying("Reduce tournament duration, or increase game clock", _.excessiveDuration))
}

object DataForm {

  def canPickName(u: User) = {
    u.count.game >= 10 && u.createdSinceDays(3) && !u.troll
  } || u.hasTitle || u.isVerified

  import draughts.variant._

  val clockTimes: Seq[Double] = Seq(0d, 1 / 4d, 1 / 2d, 3 / 4d, 1d, 3 / 2d) ++ (2d to 8d by 1d) ++ (10d to 30d by 5d) ++ (40d to 60d by 10d)
  val clockTimeDefault = 2d
  private def formatLimit(l: Double) =
    draughts.Clock.Config(l * 60 toInt, 0).limitString + {
      if (l <= 1) " minute" else " minutes"
    }
  val clockTimeChoices = optionsDouble(clockTimes, formatLimit)

  val clockIncrements = (0 to 7 by 1) ++ (10 to 30 by 5) ++ (40 to 60 by 10)
  val clockIncrementDefault = 0
  val clockIncrementChoices = options(clockIncrements, "%d second{s}")

  val minutes = (20 to 60 by 5) ++ (70 to 120 by 10) ++ (150 to 360 by 30)
  val minuteDefault = 45
  val minuteChoices = options(minutes, "%d minute{s}")

  val waitMinutes = Seq(1, 2, 3, 5, 10, 15, 20, 30, 45, 60)
  val waitMinuteChoices = options(waitMinutes, "%d minute{s}")
  val waitMinuteDefault = 5

  val validVariants = List(Standard, Frisian, Frysk, Antidraughts, Breakthrough, Russian)

  def guessVariant(from: String): Option[Variant] = validVariants.find { v =>
    v.key == from || parseIntOption(from).exists(v.id ==)
  }

  def startingPosition(fen: String, variant: Variant): StartingPosition =
    variant.openingByFen(fen) | variant.startingPosition
}

private[tournament] case class TournamentSetup(
    name: Option[String],
    clockTime: Double,
    clockIncrement: Int,
    minutes: Int,
    waitMinutes: Option[Int],
    startDate: Option[DateTime],
    variant: Option[String],
    positionStandard: Option[String],
    positionRussian: Option[String], // NOTE: Safe for variants without standard initial position (i.e. 64 squares)
    mode: Option[Int], // deprecated, use rated
    rated: Option[Boolean],
    password: Option[String],
    conditions: Condition.DataForm.AllSetup,
    berserkable: Option[Boolean],
    description: Option[String]
) {

  def validClock = (clockTime + clockIncrement) > 0

  def realMode = Mode(rated.orElse(mode.map(Mode.Rated.id ==)) | true)

  def realVariant = variant.flatMap(DataForm.guessVariant) | draughts.variant.Standard

  def clockConfig = draughts.Clock.Config((clockTime * 60).toInt, clockIncrement)

  def validRatedUltraBulletVariant =
    realMode == Mode.Casual ||
      lidraughts.game.Game.allowRated(realVariant, clockConfig)

  def sufficientDuration = estimateNumberOfGamesOneCanPlay >= 3
  def excessiveDuration = estimateNumberOfGamesOneCanPlay <= 70

  private def estimateNumberOfGamesOneCanPlay: Double = (minutes * 60) / estimatedGameSeconds

  // There are 2 players, and they don't always use all their time (0.8)
  // add 15 seconds for pairing delay
  private def estimatedGameSeconds: Double = {
    (60 * clockTime + 30 * clockIncrement) * 2 * 0.8
  } + 15
}
