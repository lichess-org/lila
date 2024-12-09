package lila.challenge

import cats.derived.*
import chess.format.Fen
import chess.variant.{ Chess960, FromPosition, Horde, RacingKings, Variant }
import chess.{ Color, Mode, Speed }
import reactivemongo.api.bson.Macros.Annotations.Key
import scalalib.ThreadLocalRandom
import scalalib.model.Days

import lila.core.challenge as hub
import lila.core.game.GameRule
import lila.core.i18n.I18nKey
import lila.core.id.GameFullId
import lila.core.user.{ GameUser, WithPerf }
import lila.rating.PerfType

case class Challenge(
    @Key("_id") id: ChallengeId,
    status: Challenge.Status,
    variant: Variant,
    initialFen: Option[Fen.Full],
    timeControl: Challenge.TimeControl,
    mode: Mode,
    colorChoice: Challenge.ColorChoice,
    finalColor: Color,
    challenger: Challenge.Challenger,
    destUser: Option[Challenge.Challenger.Registered],
    rematchOf: Option[GameId],
    createdAt: Instant,
    seenAt: Option[Instant], // None for open challenges, so they don't sweep
    expiresAt: Instant,
    open: Option[Challenge.Open] = None,
    name: Option[String] = None,
    declineReason: Option[Challenge.DeclineReason] = None,
    rules: Set[GameRule] = Set.empty
) extends hub.Challenge:

  import Challenge.*

  def gameId = id.into(GameId)

  def challengerUserId = challengerUser.map(_.id)
  def challengerIsOpen = challenger match
    case Challenger.Open => true
    case _               => false

  def userIds = List(challengerUserId, destUserId).flatten

  def daysPerTurn = timeControl match
    case TimeControl.Correspondence(d) => d.some
    case _                             => none
  def unlimited = timeControl == TimeControl.Unlimited

  def openDest = destUser.isEmpty
  def online   = status == Status.Created
  def active   = online || status == Status.Offline
  def canceled = status == Status.Canceled
  def declined = status == Status.Declined
  def accepted = status == Status.Accepted

  def setChallenger(u: GameUser, secret: Option[String]) =
    copy(
      challenger =
        u.map(toRegistered).orElse(secret.map(Challenger.Anonymous.apply)).getOrElse(Challenger.Open)
    )
  def setDestUser(u: WithPerf) = copy(destUser = toRegistered(u).some)

  def speed = speedOf(timeControl)

  def notableInitialFen: Option[Fen.Full] = variant match
    case FromPosition | Horde | RacingKings | Chess960 => initialFen
    case _                                             => none

  def isOpen = open.isDefined

  lazy val perfType = perfTypeOf(variant, timeControl)

  def anyDeclineReason = declineReason | DeclineReason.default

  def declineWith(reason: DeclineReason) = copy(
    status = Status.Declined,
    declineReason = reason.some
  )

  def cancel = copy(status = Status.Canceled)

  def isBoardCompatible: Boolean = speed >= Speed.Blitz
  def isBotCompatible: Boolean   = speed >= Speed.Bullet

  def nonEmptyRules = rules.nonEmpty.option(rules)

  def fullIdOf(game: lila.core.game.Game, direction: Direction): GameFullId =
    game.fullIdOf:
      if direction == Direction.Out then finalColor else !finalColor

object Challenge:

  export hub.Challenge.*

  enum Status(val id: Int):
    val name = Status.this.toString.toLowerCase

    case Created  extends Status(10)
    case Offline  extends Status(15)
    case Canceled extends Status(20)
    case Declined extends Status(30)
    case Accepted extends Status(40)

  object Status:
    val byId = values.mapBy(_.id)

  enum DeclineReason(val trans: I18nKey):
    val key = DeclineReason.this.toString.toLowerCase

    case Generic     extends DeclineReason(I18nKey.challenge.declineGeneric)
    case Later       extends DeclineReason(I18nKey.challenge.declineLater)
    case TooFast     extends DeclineReason(I18nKey.challenge.declineTooFast)
    case TooSlow     extends DeclineReason(I18nKey.challenge.declineTooSlow)
    case TimeControl extends DeclineReason(I18nKey.challenge.declineTimeControl)
    case Rated       extends DeclineReason(I18nKey.challenge.declineRated)
    case Casual      extends DeclineReason(I18nKey.challenge.declineCasual)
    case Standard    extends DeclineReason(I18nKey.challenge.declineStandard)
    case Variant     extends DeclineReason(I18nKey.challenge.declineVariant)
    case NoBot       extends DeclineReason(I18nKey.challenge.declineNoBot)
    case OnlyBot     extends DeclineReason(I18nKey.challenge.declineOnlyBot)

  object DeclineReason:

    val default            = Generic
    val all                = values.toList
    val byKey              = values.mapBy(_.key)
    val allExceptBot       = all.filterNot(r => r == NoBot || r == OnlyBot)
    def apply(key: String) = all.find { d => d.key == key.toLowerCase || d.trans.value == key } | Generic

  enum ColorChoice(val trans: I18nKey) derives Eq:
    case Random extends ColorChoice(I18nKey.site.randomColor)
    case White  extends ColorChoice(I18nKey.site.white)
    case Black  extends ColorChoice(I18nKey.site.black)
  object ColorChoice:
    def apply(c: Color) = c.fold[ColorChoice](White, Black)

  case class Open(userIds: Option[(UserId, UserId)]):
    def userIdList = userIds.map { (u1, u2) => List(u1, u2) }
    def canJoin(using me: Option[Me]) =
      userIdList.forall(ids => me.exists(me => ids.exists(me.is(_))))
    def colorFor(requestedColor: Option[Color])(using me: Option[Me]): Option[ColorChoice] =
      userIds.fold(requestedColor.fold(ColorChoice.Random)(ColorChoice.apply).some): (u1, u2) =>
        me.flatMap: m =>
          if m.is(u1) then ColorChoice.White.some
          else if m.is(u2) then ColorChoice.Black.some
          else none

  private def speedOf(timeControl: TimeControl) = timeControl match
    case TimeControl.Clock(config) => Speed(config)
    case _                         => Speed.Correspondence

  private def perfTypeOf(variant: Variant, timeControl: TimeControl): PerfType =
    lila.rating.PerfType(variant, speedOf(timeControl))

  private val idSize   = 8
  private def randomId = ChallengeId(ThreadLocalRandom.nextString(idSize))

  def toRegistered(u: WithPerf): Challenger.Registered =
    Challenger.Registered(u.id, Rating(u.perf.intRating, u.perf.provisional))

  def randomColor = Color.fromWhite(ThreadLocalRandom.nextBoolean())

  def makeTimeControl(clock: Option[chess.Clock.Config], days: Option[Days]): TimeControl =
    clock
      .map(TimeControl.Clock.apply)
      .orElse(days.map(TimeControl.Correspondence.apply))
      .getOrElse(TimeControl.Unlimited)

  def make(
      variant: Variant,
      initialFen: Option[Fen.Full],
      timeControl: TimeControl,
      mode: Mode,
      color: String,
      challenger: Challenger,
      destUser: GameUser,
      rematchOf: Option[GameId],
      name: Option[String] = None,
      id: Option[GameId] = None,
      openToUserIds: Option[(UserId, UserId)] = None,
      rules: Set[GameRule] = Set.empty,
      expiresAt: Option[Instant] = None
  ): Challenge =
    val (colorChoice, finalColor) = color match
      case "white" => ColorChoice.White  -> chess.White
      case "black" => ColorChoice.Black  -> chess.Black
      case _       => ColorChoice.Random -> randomColor
    val finalMode = timeControl match
      case TimeControl.Clock(clock) if !lila.core.game.allowRated(variant, clock.some) => Mode.Casual
      case _                                                                           => mode
    val isOpen = challenger == Challenge.Challenger.Open
    new Challenge(
      id = id.fold(randomId)(_.into(ChallengeId)),
      status = Status.Created,
      variant = variant,
      initialFen =
        if variant == FromPosition then initialFen
        else if variant == Chess960 then
          initialFen.filter: fen =>
            Chess960.positionNumber(fen).isDefined
        else (!variant.standardInitialPosition).option(variant.initialFen),
      timeControl = timeControl,
      mode = finalMode,
      colorChoice = colorChoice,
      finalColor = finalColor,
      challenger = challenger,
      destUser = destUser.map(toRegistered),
      rematchOf = rematchOf,
      createdAt = nowInstant,
      seenAt = (!isOpen).option(nowInstant),
      expiresAt = expiresAt | {
        if isOpen then nowInstant.plusDays(1) else inTwoWeeks
      },
      open = isOpen.option(Open(openToUserIds)),
      name = name,
      rules = rules
    )
