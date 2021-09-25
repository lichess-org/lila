package lila.tournament

import cats.implicits._
import chess.format.FEN
import chess.{ Mode, StartingPosition }
import org.joda.time.DateTime
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation
import play.api.data.validation.Constraint
import scala.util.chaining._

import lila.common.Form._
import lila.hub.LeaderTeam
import lila.hub.LightTeam._
import lila.user.User

final class TournamentForm {

  import TournamentForm._

  def create(user: User, leaderTeams: List[LeaderTeam], teamBattleId: Option[TeamID] = None) =
    form(user, leaderTeams, none) fill TournamentSetup(
      name = teamBattleId.isEmpty option user.titleUsername,
      clockTime = clockTimeDefault,
      clockIncrement = clockIncrementDefault,
      minutes = minuteDefault,
      waitMinutes = waitMinuteDefault.some,
      startDate = none,
      variant = chess.variant.Standard.id.toString.some,
      position = None,
      password = None,
      mode = none,
      rated = true.some,
      conditions = Condition.DataForm.AllSetup.default,
      teamBattleByTeam = teamBattleId,
      berserkable = true.some,
      streakable = true.some,
      description = none,
      hasChat = true.some
    )

  def edit(user: User, leaderTeams: List[LeaderTeam], tour: Tournament) =
    form(user, leaderTeams, tour.some) fill TournamentSetup(
      name = tour.name.some,
      clockTime = tour.clock.limitInMinutes,
      clockIncrement = tour.clock.incrementSeconds,
      minutes = tour.minutes,
      waitMinutes = none,
      startDate = tour.startsAt.some,
      variant = tour.variant.id.toString.some,
      position = tour.position,
      mode = none,
      rated = tour.mode.rated.some,
      password = tour.password,
      conditions = Condition.DataForm.AllSetup(tour.conditions),
      teamBattleByTeam = none,
      berserkable = tour.berserkable.some,
      streakable = tour.streakable.some,
      description = tour.description,
      hasChat = tour.hasChat.some
    )

  private val blockList = List("lichess", "liсhess")

  private def form(user: User, leaderTeams: List[LeaderTeam], prev: Option[Tournament]) =
    Form {
      makeMapping(user, leaderTeams) pipe { m =>
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
        }
      }
    }

  private def makeMapping(user: User, leaderTeams: List[LeaderTeam]) =
    mapping(
      "name"           -> optional(eventName(2, 30, user.isVerifiedOrAdmin)),
      "clockTime"      -> numberInDouble(clockTimeChoices),
      "clockIncrement" -> numberIn(clockIncrementChoices),
      "minutes" -> {
        if (lila.security.Granter(_.ManageTournament)(user)) number
        else numberIn(minuteChoices)
      },
      "waitMinutes" -> optional(numberIn(waitMinuteChoices)),
      "startDate"   -> optional(inTheFuture(ISODateTimeOrTimestamp.isoDateTimeOrTimestamp)),
      "variant"     -> optional(text.verifying(v => guessVariant(v).isDefined)),
      "position"    -> optional(lila.common.Form.fen.playableStrict),
      "mode"        -> optional(number.verifying(Mode.all.map(_.id) contains _)), // deprecated, use rated
      "rated"       -> optional(boolean),
      "password"    -> optional(cleanNonEmptyText),
      "conditions"  -> Condition.DataForm.all(leaderTeams),
      "teamBattleByTeam" -> optional(nonEmptyText.verifying(id => leaderTeams.exists(_.id == id))),
      "berserkable"      -> optional(boolean),
      "streakable"       -> optional(boolean),
      "description"      -> optional(cleanNonEmptyText),
      "hasChat"          -> optional(boolean)
    )(TournamentSetup.apply)(TournamentSetup.unapply)
      .verifying("Invalid clock", _.validClock)
      .verifying("15s and 0+1 variant games cannot be rated", _.validRatedVariant)
      .verifying("Increase tournament duration, or decrease game clock", _.sufficientDuration)
      .verifying("Reduce tournament duration, or increase game clock", _.excessiveDuration)
}

object TournamentForm {

  import chess.variant._

  val clockTimes: Seq[Double] = Seq(0d, 1 / 4d, 1 / 2d, 3 / 4d, 1d, 3 / 2d) ++ {
    (2 to 7 by 1) ++ (10 to 30 by 5) ++ (40 to 60 by 10)
  }.map(_.toDouble)
  val clockTimeDefault = 2d
  private def formatLimit(l: Double) =
    chess.Clock.Config(l * 60 toInt, 0).limitString + {
      if (l <= 1) " minute" else " minutes"
    }
  val clockTimeChoices = optionsDouble(clockTimes, formatLimit)

  val clockIncrements       = (0 to 2 by 1) ++ (3 to 7) ++ (10 to 30 by 5) ++ (40 to 60 by 10)
  val clockIncrementDefault = 0
  val clockIncrementChoices = options(clockIncrements, "%d second{s}")

  val minutes       = (20 to 60 by 5) ++ (70 to 120 by 10) ++ (150 to 360 by 30) ++ (420 to 600 by 60) :+ 720
  val minuteDefault = 45
  val minuteChoices = options(minutes, "%d minute{s}")

  val waitMinutes       = Seq(1, 2, 3, 5, 10, 15, 20, 30, 45, 60)
  val waitMinuteChoices = options(waitMinutes, "%d minute{s}")
  val waitMinuteDefault = 5

  val positions = StartingPosition.allWithInitial.map(_.fen)
  val positionChoices = StartingPosition.allWithInitial.map { p =>
    p.fen -> p.fullName
  }
  val positionDefault = StartingPosition.initial.fen

  val validVariants =
    List(Standard, Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde, RacingKings, Crazyhouse)

  def guessVariant(from: String): Option[Variant] =
    validVariants.find { v =>
      v.key == from || from.toIntOption.exists(v.id ==)
    }

  val joinForm =
    Form(
      mapping(
        "team"     -> optional(nonEmptyText),
        "password" -> optional(nonEmptyText)
      )(TournamentJoin.apply)(TournamentJoin.unapply)
    )

  case class TournamentJoin(team: Option[String], password: Option[String])
}

private[tournament] case class TournamentSetup(
    name: Option[String],
    clockTime: Double,
    clockIncrement: Int,
    minutes: Int,
    waitMinutes: Option[Int],
    startDate: Option[DateTime],
    variant: Option[String],
    position: Option[FEN],
    mode: Option[Int], // deprecated, use rated
    rated: Option[Boolean],
    password: Option[String],
    conditions: Condition.DataForm.AllSetup,
    teamBattleByTeam: Option[String],
    berserkable: Option[Boolean],
    streakable: Option[Boolean],
    description: Option[String],
    hasChat: Option[Boolean]
) {

  def validClock = (clockTime + clockIncrement) > 0

  def realMode =
    if (realPosition.isDefined) Mode.Casual
    else Mode(rated.orElse(mode.map(Mode.Rated.id ===)) | true)

  def realVariant = variant.flatMap(TournamentForm.guessVariant) | chess.variant.Standard

  def realPosition = position ifTrue realVariant.standard

  def clockConfig = chess.Clock.Config((clockTime * 60).toInt, clockIncrement)

  def speed = chess.Speed(clockConfig)

  def validRatedVariant =
    realMode == Mode.Casual ||
      lila.game.Game.allowRated(realVariant, clockConfig.some)

  def sufficientDuration = estimateNumberOfGamesOneCanPlay >= 3
  def excessiveDuration  = estimateNumberOfGamesOneCanPlay <= 150

  def isPrivate = password.isDefined || conditions.teamMember.isDefined

  // prevent berserk tournament abuse with TC like 1+60,
  // where perf is Classical but berserked games are Hyperbullet.
  def timeControlPreventsBerserk =
    clockConfig.incrementSeconds > clockConfig.limitInMinutes * 2

  def isBerserkable = ~berserkable && !timeControlPreventsBerserk

  // update all fields and use default values for missing fields
  // meant for HTML form updates
  def updateAll(old: Tournament): Tournament = {
    val newVariant = if (old.isCreated && variant.isDefined) realVariant else old.variant
    old
      .copy(
        name = name | old.name,
        clock = if (old.isCreated) clockConfig else old.clock,
        minutes = minutes,
        mode = realMode,
        variant = newVariant,
        startsAt = startDate | old.startsAt,
        password = password,
        position = newVariant.standard ?? {
          if (old.isCreated || old.position.isDefined) realPosition
          else old.position
        },
        noBerserk = !isBerserkable,
        noStreak = !(~streakable),
        teamBattle = old.teamBattle,
        description = description,
        hasChat = hasChat | true
      )
  }

  // update only fields that are specified
  // meant for API updates
  def updatePresent(old: Tournament): Tournament = {
    val newVariant = if (old.isCreated) realVariant else old.variant
    old
      .copy(
        name = name | old.name,
        clock = if (old.isCreated) clockConfig else old.clock,
        minutes = minutes,
        mode = if (rated.isDefined) realMode else old.mode,
        variant = newVariant,
        startsAt = startDate | old.startsAt,
        password = password.fold(old.password)(_.some.filter(_.nonEmpty)),
        position = newVariant.standard ?? {
          if (position.isDefined && (old.isCreated || old.position.isDefined)) realPosition
          else old.position
        },
        noBerserk = berserkable.fold(old.noBerserk)(!_) || timeControlPreventsBerserk,
        noStreak = streakable.fold(old.noStreak)(!_),
        teamBattle = old.teamBattle,
        description = description.fold(old.description)(_.some.filter(_.nonEmpty)),
        hasChat = hasChat | old.hasChat
      )
  }

  private def estimateNumberOfGamesOneCanPlay: Double = (minutes * 60) / estimatedGameSeconds

  // There are 2 players, and they don't always use all their time (0.8)
  // add 15 seconds for pairing delay
  private def estimatedGameSeconds: Double = {
    (60 * clockTime + 30 * clockIncrement) * 2 * 0.8
  } + 15
}
