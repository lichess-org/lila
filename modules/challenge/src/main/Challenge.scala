package lila.challenge

import cats.derived.*

import chess.format.Fen
import chess.variant.{ Chess960, FromPosition, Horde, RacingKings, Variant }
import chess.{ Color, Mode, Speed }
import ornicar.scalalib.ThreadLocalRandom

import lila.common.Days
import lila.game.{ Game, GameRule }
import lila.i18n.{ I18nKey, I18nKeys }
import lila.rating.PerfType
import lila.user.{ Me, User, GameUser }

case class Challenge(
    _id: Challenge.Id,
    status: Challenge.Status,
    variant: Variant,
    initialFen: Option[Fen.Epd],
    timeControl: Challenge.TimeControl,
    mode: Mode,
    colorChoice: Challenge.ColorChoice,
    finalColor: chess.Color,
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
):

  import Challenge.*

  inline def id = _id

  def challengerUser = challenger match
    case u: Challenger.Registered => u.some
    case _                        => none
  def challengerUserId = challengerUser.map(_.id)
  def challengerIsAnon = challenger match
    case _: Challenger.Anonymous => true
    case _                       => false
  def challengerIsOpen = challenger match
    case Challenger.Open => true
    case _               => false
  def destUserId = destUser.map(_.id)

  def userIds = List(challengerUserId, destUserId).flatten

  def daysPerTurn = timeControl match
    case TimeControl.Correspondence(d) => d.some
    case _                             => none
  def unlimited = timeControl == TimeControl.Unlimited

  def clock = timeControl match
    case c: TimeControl.Clock => c.some
    case _                    => none

  def hasClock = clock.isDefined

  def openDest = destUser.isEmpty
  def online   = status == Status.Created
  def active   = online || status == Status.Offline
  def canceled = status == Status.Canceled
  def declined = status == Status.Declined
  def accepted = status == Status.Accepted

  def setChallenger(u: GameUser, secret: Option[String]) =
    copy(
      challenger = u.map(toRegistered) orElse
        secret.map(Challenger.Anonymous.apply) getOrElse Challenger.Open
    )
  def setDestUser(u: User.WithPerf) = copy(destUser = toRegistered(u).some)

  def speed = speedOf(timeControl)

  def notableInitialFen: Option[Fen.Epd] = variant match
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

  def nonEmptyRules = rules.nonEmpty option rules

object Challenge:

  opaque type Id = String
  object Id extends OpaqueString[Id]

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

    case Generic     extends DeclineReason(I18nKeys.challenge.declineGeneric)
    case Later       extends DeclineReason(I18nKeys.challenge.declineLater)
    case TooFast     extends DeclineReason(I18nKeys.challenge.declineTooFast)
    case TooSlow     extends DeclineReason(I18nKeys.challenge.declineTooSlow)
    case TimeControl extends DeclineReason(I18nKeys.challenge.declineTimeControl)
    case Rated       extends DeclineReason(I18nKeys.challenge.declineRated)
    case Casual      extends DeclineReason(I18nKeys.challenge.declineCasual)
    case Standard    extends DeclineReason(I18nKeys.challenge.declineStandard)
    case Variant     extends DeclineReason(I18nKeys.challenge.declineVariant)
    case NoBot       extends DeclineReason(I18nKeys.challenge.declineNoBot)
    case OnlyBot     extends DeclineReason(I18nKeys.challenge.declineOnlyBot)

  object DeclineReason:

    val default            = Generic
    val all                = values.toList
    val byKey              = values.mapBy(_.key)
    val allExceptBot       = all.filterNot(r => r == NoBot || r == OnlyBot)
    def apply(key: String) = all.find { d => d.key == key.toLowerCase || d.trans.value == key } | Generic

  case class Rating(int: IntRating, provisional: RatingProvisional):
    def show = s"$int${if provisional.yes then "?" else ""}"
  object Rating:
    def apply(p: lila.rating.Perf): Rating = Rating(p.intRating, p.provisional)

  enum Challenger:
    case Registered(id: UserId, rating: Rating)
    case Anonymous(secret: String)
    case Open

  sealed trait TimeControl:
    def realTime: Option[chess.Clock.Config] = none
  object TimeControl:
    def make(clock: Option[chess.Clock.Config], days: Option[Days]) =
      clock.map(Clock.apply).orElse(days map Correspondence.apply).getOrElse(Unlimited)
    case object Unlimited                 extends TimeControl
    case class Correspondence(days: Days) extends TimeControl
    case class Clock(config: chess.Clock.Config) extends TimeControl:
      override def realTime = config.some
      // All durations are expressed in seconds
      export config.{ limit, increment, show }

  enum ColorChoice(val trans: I18nKey) derives Eq:
    case Random extends ColorChoice(I18nKeys.randomColor)
    case White  extends ColorChoice(I18nKeys.white)
    case Black  extends ColorChoice(I18nKeys.black)
  object ColorChoice:
    def apply(c: Color) = c.fold[ColorChoice](White, Black)

  case class Open(userIds: Option[(UserId, UserId)]):
    def userIdList = userIds.map { (u1, u2) => List(u1, u2) }
    def canJoin(using me: Option[Me]) =
      userIdList.forall(ids => me.exists(me => ids.exists(me is _)))
    def colorFor(requestedColor: Option[Color])(using me: Option[Me]): Option[ColorChoice] =
      userIds.fold(requestedColor.fold(ColorChoice.Random)(ColorChoice.apply).some): (u1, u2) =>
        me.flatMap: m =>
          if m is u1 then ColorChoice.White.some
          else if m is u2 then ColorChoice.Black.some
          else none

  private def speedOf(timeControl: TimeControl) = timeControl match
    case TimeControl.Clock(config) => Speed(config)
    case _                         => Speed.Correspondence

  private def perfTypeOf(variant: Variant, timeControl: TimeControl): PerfType =
    PerfType(variant, speedOf(timeControl))

  private val idSize = 8

  private def randomId = ThreadLocalRandom nextString idSize

  def toRegistered(u: User.WithPerf): Challenger.Registered =
    Challenger.Registered(u.id, Rating(u.perf))

  def randomColor = chess.Color.fromWhite(ThreadLocalRandom.nextBoolean())

  def make(
      variant: Variant,
      initialFen: Option[Fen.Epd],
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
      case TimeControl.Clock(clock) if !lila.game.Game.allowRated(variant, clock.some) => Mode.Casual
      case _                                                                           => mode
    val isOpen = challenger == Challenge.Challenger.Open
    new Challenge(
      _id = id.map(_.value) | randomId,
      status = Status.Created,
      variant = variant,
      initialFen =
        if variant == FromPosition then initialFen
        else if variant == Chess960 then
          initialFen.filter: fen =>
            Chess960.positionNumber(fen).isDefined
        else !variant.standardInitialPosition option variant.initialFen,
      timeControl = timeControl,
      mode = finalMode,
      colorChoice = colorChoice,
      finalColor = finalColor,
      challenger = challenger,
      destUser = destUser map toRegistered,
      rematchOf = rematchOf,
      createdAt = nowInstant,
      seenAt = !isOpen option nowInstant,
      expiresAt = expiresAt | {
        if isOpen then nowInstant.plusDays(1) else inTwoWeeks
      },
      open = isOpen option Open(openToUserIds),
      name = name,
      rules = rules
    )
