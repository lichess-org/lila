package lila.tournament

import cats.implicits._
import shogi.format.forsyth.Sfen
import shogi.Mode
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation
import play.api.data.validation.{ Constraint, Constraints }
import scala.util.chaining._

import lila.common.Form._
import lila.hub.LightTeam._
import lila.user.User

final class DataForm {

  import DataForm._

  def create(user: User, teamBattleId: Option[TeamID] = None) =
    form(user, none) fill TournamentSetup(
      name = teamBattleId.isEmpty option user.titleUsername,
      format = (if (teamBattleId.isDefined) Format.Arena.key else Format.Robin.key).some,
      timeControlSetup = TimeControl.DataForm.Setup.default,
      minutes = minuteDefault,
      startDate = none,
      finishDate = none,
      variant = shogi.variant.Standard.id.toString.some,
      position = None,
      password = None,
      candidatesOnly = None,
      mode = none,
      rated = true.some,
      conditions = Condition.DataForm.AllSetup.default,
      teamBattleByTeam = teamBattleId,
      berserkable = true.some,
      streakable = true.some,
      description = none,
      hasChat = true.some
    )

  def edit(user: User, tour: Tournament) =
    form(user, tour.some) fill TournamentSetup(
      name = tour.name.some,
      format = tour.format.key.some,
      timeControlSetup = TimeControl.DataForm.Setup(tour.timeControl),
      minutes = tour.minutes,
      startDate = tour.startsAt.some,
      finishDate = tour.finishesAt.some,
      variant = tour.variant.id.toString.some,
      position = tour.position,
      mode = none,
      rated = tour.mode.rated.some,
      password = tour.password,
      candidatesOnly = tour.candidatesOnly.some,
      conditions = Condition.DataForm.AllSetup(tour.conditions),
      teamBattleByTeam = none,
      berserkable = tour.berserkable.some,
      streakable = tour.streakable.some,
      description = tour.description,
      hasChat = tour.hasChat.some
    )

  private val nameType = cleanText.verifying(
    Constraints minLength 2,
    Constraints maxLength 30,
    Constraints.pattern(
      regex = """[\p{L}\p{N}-\s:,;]+""".r,
      error = "error.unknown"
    ),
    Constraint[String] { (t: String) =>
      if (t.toLowerCase contains "lishogi")
        validation.Invalid(validation.ValidationError("Must not contain \"lishogi\""))
      else validation.Valid
    }
  )

  private def form(user: User, prev: Option[Tournament]) =
    Form {
      makeMapping(user) pipe { m =>
        prev.fold(m) { tour =>
          m
            .verifying(
              "Can't change variant after players have joined",
              _.realVariant == tour.variant || tour.nbPlayers == 0
            )
            .verifying(
              "Can't change time control after players have joined",
              _.speed == tour.speed || tour.nbPlayers == 0
            )
            .verifying(
              "Can't change format after tournament is created",
              _.realFormat == tour.format
            )
            .verifying(
              "Can't change tournament from 'candidates only' to open, if candidates list is not empty",
              ~_.candidatesOnly == tour.candidatesOnly || !tour.candidates.exists(_.nonEmpty)
            )
        }
      }
    }

  private def makeMapping(user: User) =
    mapping(
      "name"             -> optional(nameType),
      "format"           -> optional(nonEmptyText),
      "timeControlSetup" -> TimeControl.DataForm.setup,
      "minutes" -> {
        if (lila.security.Granter(_.ManageTournament)(user)) number
        else numberIn(minutes)
      },
      "startDate"      -> optional(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp),
      "finishDate"     -> optional(inTheFuture(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp)),
      "variant"        -> optional(text.verifying(v => guessVariant(v).isDefined)),
      "position"       -> optional(lila.common.Form.sfen.clean),
      "mode"           -> optional(number.verifying(Mode.all.map(_.id) contains _)), // deprecated, use rated
      "rated"          -> optional(boolean),
      "password"       -> optional(nonEmptyText),
      "candidatesOnly" -> optional(boolean),
      "conditions"     -> Condition.DataForm.all,
      "teamBattleByTeam" -> optional(nonEmptyText),
      "berserkable"      -> optional(boolean),
      "streakable"       -> optional(boolean),
      "description"      -> optional(cleanNonEmptyText),
      "hasChat"          -> optional(boolean)
    )(TournamentSetup.apply)(TournamentSetup.unapply)
      .verifying("Invalid starting position", _.validPosition)
      .verifying("End date needs to come at least 20 minutes after start date", _.validFinishDate)
      .verifying("Games with this time control cannot be rated", _.validRatedVariant)
      .verifying("Cannot have correspondence in arena format", _.validTimeControl)
      .verifying("Increase tournament duration, or decrease game clock", _.validSufficientDuration)
      .verifying("Reduce tournament duration, or increase game clock", _.validNotExcessiveDuration)
      .verifying("Team battle supports only arena format", _.validFormat)
      .verifying("Team battle doesn't support candidates only option", _.validCandidates)
}

object DataForm {

  import shogi.variant._

  val formats = Format.all.map(_.key)

  val minutes       = (20 to 60 by 5) ++ (70 to 120 by 10) ++ (150 to 360 by 30) ++ (420 to 600 by 60) :+ 720
  val minuteDefault = 45

  val validVariants =
    List(Standard, Minishogi, Chushogi, Annanshogi, Kyotoshogi, Checkshogi)

  def guessVariant(from: String): Option[Variant] =
    validVariants.find { v =>
      v.key == from || from.toIntOption.exists(v.id ==)
    }
}

private[tournament] case class TournamentSetup(
    name: Option[String],
    format: Option[String],
    timeControlSetup: TimeControl.DataForm.Setup,
    minutes: Int,
    startDate: Option[DateTime],
    finishDate: Option[DateTime],
    variant: Option[String],
    position: Option[Sfen],
    mode: Option[Int], // deprecated, use rated
    rated: Option[Boolean],
    password: Option[String],
    candidatesOnly: Option[Boolean],
    conditions: Condition.DataForm.AllSetup,
    teamBattleByTeam: Option[String],
    berserkable: Option[Boolean],
    streakable: Option[Boolean],
    description: Option[String],
    hasChat: Option[Boolean]
) {

  def validFormat = format == Format.Arena || teamBattleByTeam.isEmpty

  def validCandidates = !(~candidatesOnly) || teamBattleByTeam.isEmpty

  def validPosition = position.fold(true) { sfen =>
    sfen.toSituation(realVariant).exists(_.playable(strict = true, withImpasse = true))
  }

  def validFinishDate = finishDate.fold(true) { d =>
    d.minusMinutes(20) isAfter (realStartDate)
  }

  def validTimeControl = timeControlSetup.isRealTime || format != Format.Arena

  def realMode =
    if (position.filterNot(_.initialOf(realVariant)).isDefined) Mode.Casual
    else Mode(rated.orElse(mode.map(Mode.Rated.id ===)) | true)

  def realVariant = variant.flatMap(DataForm.guessVariant) | shogi.variant.Standard

  def realFormat = format.flatMap(Format.byKey) | Format.Arena

  def realStartDate = startDate.filter(_ isAfter DateTime.now).getOrElse(DateTime.now)

  def realMinutes = finishDate.ifTrue(format != Format.Arena).map { fd =>
    ((fd.getMillis - realStartDate.getMillis) / 60000).toInt
  } getOrElse minutes

  def speed = timeControlSetup.clock.fold[shogi.Speed](shogi.Speed.Correspondence)(shogi.Speed.apply)

  def validRatedVariant =
    realMode == Mode.Casual ||
      lila.game.Game.allowRated(position, timeControlSetup.clock, realVariant)

  def validSufficientDuration =
    if (timeControlSetup.isRealTime)
      estimateNumberOfGamesOneCanPlay >= 3
    else realMinutes > 180

  def validNotExcessiveDuration =
    realMinutes <= minutesMax && {
      format != Format.Arena || !timeControlSetup.isRealTime || estimateNumberOfGamesOneCanPlay <= 150
    }

  def isPrivate = password.isDefined || conditions.teamMember.isDefined

  private def minutesMax = if (format == Format.Arena) DataForm.minutes.last else 24 * 60 * 365 / 3

  private def estimateNumberOfGamesOneCanPlay: Double =
    (realMinutes * 60) / estimatedGameSeconds

  // There are 2 players, and they don't always use all their time (0.8)
  // add 15 seconds for pairing delay
  private def estimatedGameSeconds: Double = {
    (60 * timeControlSetup.clockTime + 30 * timeControlSetup.clockIncrement + timeControlSetup.clockByoyomi * 20 * timeControlSetup.periods) * 2 * 0.8
  } + 15

}
