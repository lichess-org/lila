package lila.tournament

import chess.Clock.{ IncrementSeconds, LimitSeconds }
import chess.format.Fen
import chess.{ Clock, Rated }
import play.api.data.*
import play.api.data.Forms.*

import lila.common.Form.{ *, given }
import lila.core.perm.Granter
import lila.core.team.LightTeam
import lila.gathering.GatheringClock

final class TournamentForm:

  import TournamentForm.*
  import GatheringClock.*

  def create(leaderTeams: List[LightTeam], teamBattleId: Option[TeamId] = None)(using me: Me) =
    form(leaderTeams, none).fill(empty(teamBattleId))

  private[tournament] def empty(teamBattleId: Option[TeamId] = None)(using me: Me) =
    TournamentSetup(
      name = teamBattleId.isEmpty.option(me.titleUsername),
      clockTime = timeDefault,
      clockIncrement = incrementDefault,
      minutes = minuteDefault,
      waitMinutes = waitMinuteDefault.some,
      startDate = none,
      variant = chess.variant.Standard.id.toString.some,
      position = None,
      password = None,
      rated = Rated.Yes.some,
      conditions = TournamentCondition.All.empty,
      teamBattleByTeam = teamBattleId,
      berserkable = true.some,
      streakable = true.some,
      description = none,
      hasChat = true.some
    )

  def edit(leaderTeams: List[LightTeam], tour: Tournament)(using Me) =
    form(leaderTeams, tour.some).fill(fillFromTour(tour))

  private[tournament] def fillFromTour(tour: Tournament) =
    TournamentSetup(
      name = tour.name.some,
      clockTime = tour.clock.limitInMinutes,
      clockIncrement = tour.clock.incrementSeconds,
      minutes = tour.minutes,
      waitMinutes = none,
      startDate = tour.startsAt.some,
      variant = tour.variant.id.toString.some,
      position = tour.position.map(_.into(Fen.Full)),
      rated = tour.rated.some,
      password = tour.password,
      conditions = tour.conditions,
      teamBattleByTeam = none,
      berserkable = tour.berserkable.some,
      streakable = tour.streakable.some,
      description = tour.description,
      hasChat = tour.hasChat.some
    )

  private def form(leaderTeams: List[LightTeam], prev: Option[Tournament])(using Me) =
    Form:
      val m = makeMapping(leaderTeams, prev)
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
          .verifying(
            "Can't change bot entry condition after the tournament started",
            d => (d.conditions.allowsBots == tour.conditions.allowsBots) || tour.isCreated
          )

  private def makeMapping(leaderTeams: List[LightTeam], prev: Option[Tournament])(using me: Me) =
    val manager = Granter(_.ManageTournament)
    val nameMaxLength = if me.isVerified || manager then 35 else 30
    mapping(
      "name" -> optional(eventName(2, nameMaxLength, manager || me.isVerified)),
      "clockTime" -> numberInDouble(timeChoices),
      "clockIncrement" -> numberIn(incrementChoices).into[IncrementSeconds],
      "minutes" -> {
        if manager then number
        else numberIn(minuteChoicesKeepingCustom(prev))
      },
      "waitMinutes" -> optional(numberIn(waitMinuteChoices)),
      "startDate" -> optional(inTheFuture(ISOInstantOrTimestamp.mapping)),
      "variant" -> optional(text.verifying(v => guessVariant(v).isDefined)),
      "position" -> optional(lila.common.Form.fen.playableStrict),
      "rated" -> optional(boolean.into[Rated]),
      "password" -> optional(cleanNonEmptyText),
      "conditions" -> TournamentCondition.form.all(leaderTeams),
      "teamBattleByTeam" -> optional(of[TeamId].verifying(id => leaderTeams.exists(_.id == id))),
      "berserkable" -> optional(boolean),
      "streakable" -> optional(boolean),
      "description" -> optional(cleanNonEmptyText),
      "hasChat" -> optional(boolean)
    )(TournamentSetup.apply)(unapply)
      .verifying("Invalid clock", _.validClock)
      .verifying("Invalid clock for bot games", _.validClockForBots)
      .verifying("15s and 0+1 variant games cannot be rated", _.validRatedVariant)
      .verifying("Increase tournament duration, or decrease game clock", _.sufficientDuration)
      .verifying("Reduce tournament duration, or increase game clock", _.excessiveDuration)

object TournamentForm:

  import chess.variant.*

  val minutes = (20 to 60 by 5) ++ (70 to 120 by 10) ++ (150 to 360 by 30) ++ (420 to 600 by 60) :+ 720
  val minuteDefault = 45
  val minuteChoices = options(minutes, "%d minute{s}")
  def minuteChoicesKeepingCustom(prev: Option[Tournament]) = prev.fold(minuteChoices): tour =>
    if minuteChoices.exists(_._1 == tour.minutes) then minuteChoices
    else minuteChoices ++ List(tour.minutes -> s"${tour.minutes} minutes")

  val waitMinutes = Seq(1, 2, 3, 5, 10, 15, 20, 30, 45, 60)
  val waitMinuteChoices = options(waitMinutes, "%d minute{s}")
  val waitMinuteDefault = 5

  val validVariants =
    List(Standard, Chess960, KingOfTheHill, ThreeCheck, Antichess, Atomic, Horde, RacingKings, Crazyhouse)

  def guessVariant(from: String): Option[Variant] =
    validVariants.find: v =>
      v.key.value == from || from.toIntOption.exists(v.id.value == _)

  val joinForm = Form:
    mapping(
      "team" -> optional(nonEmptyText.into[TeamId]),
      "password" -> optional(nonEmptyText),
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
    position: Option[Fen.Full],
    rated: Option[Rated],
    password: Option[String],
    conditions: TournamentCondition.All,
    teamBattleByTeam: Option[TeamId],
    berserkable: Option[Boolean],
    streakable: Option[Boolean],
    description: Option[String],
    hasChat: Option[Boolean]
):

  def validClock = (clockTime + clockIncrement.value) > 0

  def validClockForBots = !conditions.allowsBots || lila.core.game.isBotCompatible(clockConfig)

  def realRated: Rated =
    if realPosition.isDefined && !thematicPosition then Rated.No
    else rated | Rated.Yes

  def realVariant = variant.flatMap(TournamentForm.guessVariant) | chess.variant.Standard

  def realPosition: Option[Fen.Standard] = position.ifTrue(realVariant.standard).map(_.opening)
  def thematicPosition = realPosition.flatMap(lila.gathering.Thematic.byFen).isDefined

  def clockConfig = Clock.Config(LimitSeconds((clockTime * 60).toInt), clockIncrement)

  def speed = chess.Speed(clockConfig)

  def validRatedVariant =
    realRated.no || lila.core.game.allowRated(realVariant, clockConfig.some)

  def sufficientDuration = estimateNumberOfGamesOneCanPlay >= 3
  def excessiveDuration = estimateNumberOfGamesOneCanPlay <= 150

  def isPrivate = password.isDefined || conditions.teamMember.isDefined

  // prevent berserk tournament abuse with TC like 1+60,
  // where perf is Classical but berserked games are Hyperbullet.
  def timeControlPreventsBerserk =
    clockConfig.incrementSeconds.value > clockConfig.limitInMinutes * 2

  def isBerserkable = ~berserkable && !timeControlPreventsBerserk

  // update all fields and use default values for missing fields
  // meant for HTML form updates
  def updateAll(old: Tournament): Tournament =
    val newVariant = if old.isCreated && variant.isDefined then realVariant else old.variant
    old
      .copy(
        name = name | old.name,
        clock = if old.isCreated then clockConfig else old.clock,
        minutes = minutes,
        rated = realRated,
        variant = newVariant,
        startsAt = startDate | old.startsAt,
        password = password,
        position = newVariant.standard.so:
          if old.isCreated || old.position.isDefined then realPosition
          else old.position
        ,
        noBerserk = !isBerserkable,
        noStreak = !(~streakable),
        teamBattle = old.teamBattle,
        description = description,
        hasChat = hasChat | true
      )

  // update only fields that are specified
  // meant for API updates
  def updatePresent(old: Tournament): Tournament =
    val newVariant = if old.isCreated then realVariant else old.variant
    old
      .copy(
        name = name | old.name,
        clock = if old.isCreated then clockConfig else old.clock,
        minutes = minutes,
        rated = if rated.isDefined then realRated else old.rated,
        variant = newVariant,
        startsAt = startDate | old.startsAt,
        password = password.fold(old.password)(_.some.filter(_.nonEmpty)),
        position = newVariant.standard.so {
          if position.isDefined && (old.isCreated || old.position.isDefined) then realPosition
          else old.position
        },
        noBerserk = berserkable.fold(old.noBerserk)(!_) || timeControlPreventsBerserk,
        noStreak = streakable.fold(old.noStreak)(!_),
        teamBattle = old.teamBattle,
        description = description.fold(old.description)(_.some.filter(_.nonEmpty)),
        hasChat = hasChat | old.hasChat
      )

  private def estimateNumberOfGamesOneCanPlay: Double = (minutes.atMost(720) * 60) / estimatedGameSeconds

  // There are 2 players, and they don't always use all their time (0.8)
  // add 15 seconds for pairing delay
  private def estimatedGameSeconds: Double = {
    (60 * clockTime + 30 * clockIncrement.value) * 2 * 0.8
  } + 15
