package lila.tournament

import cats.syntax.all.*
import chess.format.Fen
import chess.{ Clock, Mode }
import chess.Clock.{ LimitSeconds, IncrementSeconds }
import play.api.data.*
import play.api.data.Forms.*
import scala.util.chaining.*

import lila.common.Form.{ *, given }
import lila.hub.LeaderTeam
import lila.user.Me
import lila.gathering.GatheringClock

final class TournamentForm:

  import TournamentForm.*
  import GatheringClock.*

  def create(leaderTeams: List[LeaderTeam], teamBattleId: Option[TeamId] = None)(using me: Me) =
    form(leaderTeams, none) fill TournamentSetup(
      name = teamBattleId.isEmpty option me.titleUsername,
      clockTime = timeDefault,
      clockIncrement = incrementDefault,
      minutes = minuteDefault,
      waitMinutes = waitMinuteDefault.some,
      startDate = none,
      variant = chess.variant.Standard.id.toString.some,
      position = None,
      password = None,
      mode = none,
      rated = true.some,
      conditions = TournamentCondition.All.empty,
      teamBattleByTeam = teamBattleId,
      berserkable = true.some,
      streakable = true.some,
      description = none,
      hasChat = true.some
    )

  def edit(leaderTeams: List[LeaderTeam], tour: Tournament)(using Me) =
    form(leaderTeams, tour.some) fill TournamentSetup(
      name = tour.name.some,
      clockTime = tour.clock.limitInMinutes,
      clockIncrement = tour.clock.incrementSeconds,
      minutes = tour.minutes,
      waitMinutes = none,
      startDate = tour.startsAt.some,
      variant = tour.variant.id.toString.some,
      position = tour.position.map(_ into Fen.Epd),
      mode = none,
      rated = tour.mode.rated.some,
      password = tour.password,
      conditions = tour.conditions,
      teamBattleByTeam = none,
      berserkable = tour.berserkable.some,
      streakable = tour.streakable.some,
      description = tour.description,
      hasChat = tour.hasChat.some
    )

  private def form(leaderTeams: List[LeaderTeam], prev: Option[Tournament])(using Me) =
    Form:
      makeMapping(leaderTeams).pipe: m =>
        prev.fold(m): tour =>
          m
            .verifying(
              "Can't change variant after players have joined",
              _.realVariant == tour.variant || tour.nbPlayers == 0
            )
            .verifying(
              "Can't change time control after players have joined",
              _.speed == tour.speed || tour.nbPlayers == 0
            )

  private def makeMapping(leaderTeams: List[LeaderTeam])(using me: Me) =
    mapping(
      "name"           -> optional(eventName(2, 30, me.isVerifiedOrAdmin)),
      "clockTime"      -> numberInDouble(timeChoices),
      "clockIncrement" -> numberIn(incrementChoices).into[IncrementSeconds],
      "minutes" -> {
        if lila.security.Granter(_.ManageTournament) then number
        else numberIn(minuteChoices)
      },
      "waitMinutes" -> optional(numberIn(waitMinuteChoices)),
      "startDate"   -> optional(inTheFuture(ISOInstantOrTimestamp.mapping)),
      "variant"     -> optional(text.verifying(v => guessVariant(v).isDefined)),
      "position"    -> optional(lila.common.Form.fen.playableStrict),
      "mode"        -> optional(number.verifying(Mode.all.map(_.id) contains _)), // deprecated, use rated
      "rated"       -> optional(boolean),
      "password"    -> optional(cleanNonEmptyText),
      "conditions"  -> TournamentCondition.form.all(leaderTeams),
      "teamBattleByTeam" -> optional(of[TeamId].verifying(id => leaderTeams.exists(_.id == id))),
      "berserkable"      -> optional(boolean),
      "streakable"       -> optional(boolean),
      "description"      -> optional(cleanNonEmptyText),
      "hasChat"          -> optional(boolean)
    )(TournamentSetup.apply)(unapply)
      .verifying("Invalid clock", _.validClock)
      .verifying("15s and 0+1 variant games cannot be rated", _.validRatedVariant)
      .verifying("Increase tournament duration, or decrease game clock", _.sufficientDuration)
      .verifying("Reduce tournament duration, or increase game clock", _.excessiveDuration)

object TournamentForm:

  import chess.variant.*

  val minutes       = (20 to 60 by 5) ++ (70 to 120 by 10) ++ (150 to 360 by 30) ++ (420 to 600 by 60) :+ 720
  val minuteDefault = 45
  val minuteChoices = options(minutes, "%d minute{s}")

  val waitMinutes       = Seq(1, 2, 3, 5, 10, 15, 20, 30, 45, 60)
  val waitMinuteChoices = options(waitMinutes, "%d minute{s}")
  val waitMinuteDefault = 5

  val validVariants =
    List(Standard, Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde, RacingKings, Crazyhouse)

  def guessVariant(from: String): Option[Variant] =
    validVariants.find: v =>
      v.key.value == from || from.toIntOption.exists(v.id.value == _)

  val joinForm = Form:
    mapping(
      "team"       -> optional(nonEmptyText.into[TeamId]),
      "password"   -> optional(nonEmptyText),
      "pairMeAsap" -> optional(boolean)
    )(TournamentJoin.apply)(unapply)

  case class TournamentJoin(
      team: Option[TeamId],
      password: Option[String],
      pairMeAsap: Option[Boolean] = None
  )

private[tournament] case class TournamentSetup(
    name: Option[String],
    clockTime: Double,
    clockIncrement: IncrementSeconds,
    minutes: Int,
    waitMinutes: Option[Int],
    startDate: Option[Instant],
    variant: Option[String],
    position: Option[Fen.Epd],
    mode: Option[Int], // deprecated, use rated
    rated: Option[Boolean],
    password: Option[String],
    conditions: TournamentCondition.All,
    teamBattleByTeam: Option[TeamId],
    berserkable: Option[Boolean],
    streakable: Option[Boolean],
    description: Option[String],
    hasChat: Option[Boolean]
):

  def validClock = (clockTime + clockIncrement.value) > 0

  def realMode =
    if (realPosition.isDefined) Mode.Casual
    else Mode(rated.orElse(mode.map(Mode.Rated.id ===)) | true)

  def realVariant = variant.flatMap(TournamentForm.guessVariant) | chess.variant.Standard

  def realPosition: Option[Fen.Opening] = position.ifTrue(realVariant.standard).map(_.opening)

  def clockConfig = Clock.Config(LimitSeconds((clockTime * 60).toInt), clockIncrement)

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
    clockConfig.incrementSeconds.value > clockConfig.limitInMinutes * 2

  def isBerserkable = ~berserkable && !timeControlPreventsBerserk

  // update all fields and use default values for missing fields
  // meant for HTML form updates
  def updateAll(old: Tournament): Tournament =
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
        position = newVariant.standard so {
          if (old.isCreated || old.position.isDefined) realPosition
          else old.position
        },
        noBerserk = !isBerserkable,
        noStreak = !(~streakable),
        teamBattle = old.teamBattle,
        description = description,
        hasChat = hasChat | true
      )

  // update only fields that are specified
  // meant for API updates
  def updatePresent(old: Tournament): Tournament =
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
        position = newVariant.standard so {
          if (position.isDefined && (old.isCreated || old.position.isDefined)) realPosition
          else old.position
        },
        noBerserk = berserkable.fold(old.noBerserk)(!_) || timeControlPreventsBerserk,
        noStreak = streakable.fold(old.noStreak)(!_),
        teamBattle = old.teamBattle,
        description = description.fold(old.description)(_.some.filter(_.nonEmpty)),
        hasChat = hasChat | old.hasChat
      )

  private def estimateNumberOfGamesOneCanPlay: Double = (minutes * 60) / estimatedGameSeconds

  // There are 2 players, and they don't always use all their time (0.8)
  // add 15 seconds for pairing delay
  private def estimatedGameSeconds: Double = {
    (60 * clockTime + 30 * clockIncrement.value) * 2 * 0.8
  } + 15
