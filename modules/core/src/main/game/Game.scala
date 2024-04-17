package lila.core
package game

import scalalib.model.Days
import _root_.chess.Color.{ Black, White }
import _root_.chess.MoveOrDrop.{ color, fold }
import _root_.chess.format.pgn.SanStr
import _root_.chess.format.{ Fen, Uci }
import _root_.chess.opening.{ Opening, OpeningDb }
import _root_.chess.variant.{ FromPosition, Standard, Variant }
import _root_.chess.{
  ByColor,
  Castles,
  Centis,
  CheckCount,
  Clock,
  Color,
  Game as ChessGame,
  Mode,
  MoveOrDrop,
  Outcome,
  Ply,
  Speed,
  Status
}
import lila.core.id.{ GameId, GamePlayerId, GameFullId }
import lila.core.userId.{ UserId, UserIdOf }
import lila.core.user.User
import lila.core.perf.PerfKey

case class Game(
    id: GameId,
    players: ByColor[Player],
    chess: ChessGame,
    loadClockHistory: Clock => Option[ClockHistory] = _ => ClockHistory.someEmpty,
    status: Status,
    daysPerTurn: Option[Days],
    binaryMoveTimes: Option[Array[Byte]] = None,
    mode: Mode = Mode.default,
    bookmarks: Int = 0,
    createdAt: Instant = nowInstant,
    movedAt: Instant = nowInstant,
    metadata: GameMetadata
):

  export chess.{ situation, ply, clock, sans, startedAtPly, player as turnColor }
  export chess.situation.board
  export chess.situation.board.{ history, variant }
  export metadata.{ tournamentId, simulId, swissId, drawOffers, source, pgnImport, hasRule }
  export players.{ white as whitePlayer, black as blackPlayer, apply as player }

  lazy val clockHistory = chess.clock.flatMap(loadClockHistory)

  def drawOfferPlies                                   = drawOffers.normalizedPlies
  def player(playerId: GamePlayerId): Option[Player]   = players.find(_.id == playerId)
  def player[U: UserIdOf](user: U): Option[Player]     = players.find(_.isUser(user))
  def opponentOf[U: UserIdOf](user: U): Option[Player] = player(user).map(opponent)

  def player: Player = players(turnColor)

  def hasUserIds(userId1: UserId, userId2: UserId) =
    hasUserId(userId1) && hasUserId(userId2)

  def hasUserId(userId: UserId) = players.exists(_.userId.has(userId))

  def userIdPair: ByColor[Option[UserId]] = players.map(_.userId)

  def opponent(p: Player): Player = opponent(p.color)
  def opponent(c: Color): Player  = player(!c)

  lazy val naturalOrientation =
    if variant.racingKings then White else Color.fromWhite(players.reduce(_.before(_)))

  def turnOf(p: Player): Boolean = p == player
  def turnOf(c: Color): Boolean  = c == turnColor
  def turnOf(u: User): Boolean   = player(u).exists(turnOf)

  def playedTurns = ply - startedAtPly

  def flagged = (status == Status.Outoftime).option(turnColor)

  def fullIdOf(player: Player): Option[GameFullId] =
    players.contains(player).option(GameFullId(s"$id${player.id}"))

  def fullIdOf(color: Color) = GameFullId(s"$id${player(color).id}")

  def fullIds: ByColor[GameFullId] = ByColor(fullIdOf)

  export tournamentId.{ isDefined as isTournament }
  export simulId.{ isDefined as isSimul }
  export swissId.{ isDefined as isSwiss }
  def isMandatory          = isTournament || isSimul || isSwiss
  def nonMandatory         = !isMandatory
  def canTakebackOrAddTime = !isMandatory

  def hasChat = !isTournament && !isSimul && !isSwiss && nonAi

  // we can't rely on the clock,
  // because if moretime was given,
  // elapsed time is no longer representing the game duration
  def durationSeconds: Option[Int] =
    val seconds = movedAt.toSeconds - createdAt.toSeconds
    (seconds < 60 * 60 * 12).option( // no way it lasted more than 12 hours, come on.
      seconds.toInt
    )

  def bothClockStates: Option[Vector[Centis]] = clockHistory.map(_.bothClockStates(startColor))

  def sansOf(color: Color): Vector[SanStr] =
    val pivot = if color == startColor then 0 else 1
    sans.zipWithIndex.collect:
      case (e, i) if (i % 2) == pivot => e

  def lastMoveKeys: Option[String] =
    history.lastMove.map:
      case Uci.Drop(_, target) => s"${target.key}${target.key}"
      case m: Uci.Move         => m.keys

  def updatePlayer(color: Color, f: Player => Player) =
    copy(players = players.update(color, f))

  def updatePlayers(f: Player => Player) =
    copy(players = players.map(f))

  def start =
    if started then this
    else
      copy(
        status = Status.Started,
        mode = Mode(mode.rated && userIds.distinct.size == 2)
      )

  def correspondenceClock: Option[CorrespondenceClock] =
    daysPerTurn.map: days =>
      val increment   = days.value * 24 * 60 * 60
      val secondsLeft = (movedAt.toSeconds + increment - nowSeconds).toInt.max(0)
      CorrespondenceClock(
        increment = increment,
        whiteTime = turnColor.fold(secondsLeft, increment).toFloat,
        blackTime = turnColor.fold(increment, secondsLeft).toFloat
      )

  def playableCorrespondenceClock: Option[CorrespondenceClock] =
    if playable then correspondenceClock else none

  def speed = Speed(chess.clock.map(_.config))

  def perfKey: PerfKey = PerfKey(variant, speed)

  def ratingVariant: Variant =
    if isTournament && variant.fromPosition then Standard else variant

  def started = status >= Status.Started

  def notStarted = !started

  def aborted = status == Status.Aborted

  def playedThenAborted = aborted && bothPlayersHaveMoved

  def abort = copy(status = Status.Aborted)

  def playable = status < Status.Aborted && !sourceIs(_.Import)

  def playableEvenImported = status < Status.Aborted

  def playableBy(p: Player): Boolean = playable && turnOf(p)
  def playableBy(c: Color): Boolean  = playable && turnOf(c)

  def playableByAi: Boolean = playable && player.isAi

  def mobilePushable = isCorrespondence && playable && nonAi

  def alarmable = hasCorrespondenceClock && playable && nonAi

  def continuable = status != Status.Mate && status != Status.Stalemate

  def aiLevel: Option[Int] = players.find(_.aiLevel)

  def hasAi: Boolean = players.exists(_.isAi)
  def nonAi          = !hasAi

  def synthetic = id == GameId("synthetic")

  def aiPov: Option[Pov] = players.collect { case x if x.isAi => x.color }.map(pov)

  def mapPlayers(f: Player => Player) =
    copy(players = players.map(f))

  def playerCanOfferDraw(color: Color) =
    started && playable &&
      ply >= 2 &&
      !player(color).isOfferingDraw &&
      !opponent(color).isAi &&
      !playerHasOfferedDrawRecently(color) &&
      !swissPreventsDraw &&
      !rulePreventsDraw

  def swissPreventsDraw = isSwiss && playedTurns < 60
  def rulePreventsDraw  = hasRule(_.noEarlyDraw) && playedTurns < 60

  def playerHasOfferedDrawRecently(color: Color) =
    drawOffers.lastBy(color).exists(_ >= ply - 20)

  def offerDraw(color: Color) = copy(
    metadata = metadata.copy(drawOffers = drawOffers.add(color, ply))
  ).updatePlayer(color, _.offerDraw)

  def playerCouldRematch =
    finishedOrAborted &&
      nonMandatory &&
      !hasRule(_.noRematch) &&
      !boosted &&
      !(hasAi && variant == FromPosition && clock.exists(_.config.limitSeconds < 60))

  def playerCanProposeTakeback(color: Color) =
    started && playable && !isTournament && !isSimul &&
      bothPlayersHaveMoved &&
      !player(color).isProposingTakeback &&
      !opponent(color).isProposingTakeback

  def boosted = rated && finished && bothPlayersHaveMoved && playedTurns < 10

  def moretimeable(color: Color) =
    playable && canTakebackOrAddTime && !hasRule(_.noGiveTime) && {
      clock.exists(_.moretimeable(color)) || correspondenceClock.exists(_.moretimeable(color))
    }

  def abortable       = status == Status.Started && playedTurns < 2 && nonMandatory
  def abortableByUser = abortable && !hasRule(_.noAbort)

  def berserkable =
    isTournament && clock.exists(_.config.berserkable) && status == Status.Started && playedTurns < 2

  def resignable = playable && !abortable
  def forceResignable =
    resignable && nonAi && !sourceIs(_.Friend) && hasClock && !isSwiss && !hasRule(_.noClaimWin)
  def forceResignableNow = forceResignable && bothPlayersHaveMoved
  def drawable           = playable && !abortable && !swissPreventsDraw && !rulePreventsDraw
  def forceDrawable      = playable && nonAi && !abortable && !isSwiss && !hasRule(_.noClaimWin)

  export mode.{ rated, casual }

  def finished = status >= Status.Mate

  def finishedOrAborted = finished || aborted

  def accountable = playedTurns >= 2 || isTournament

  def replayable = isPgnImport || finished || (aborted && bothPlayersHaveMoved)

  def fromPosition = variant.fromPosition || source.has(Source.Position)

  def sourceIs(f: Source.type => Source): Boolean = source contains f(Source)

  def winner = players.find(_.wins)

  def loser = winner.map(opponent)

  def winnerColor: Option[Color] = winner.map(_.color)

  def outcome: Option[Outcome] = finished.option(Outcome(winnerColor))

  def winnerUserId: Option[UserId] = winner.flatMap(_.userId)

  def loserUserId: Option[UserId] = loser.flatMap(_.userId)

  def wonBy(c: Color): Option[Boolean] = winner.map(_.color == c)

  def lostBy(c: Color): Option[Boolean] = winner.map(_.color != c)

  def drawn = finished && winner.isEmpty

  def outoftime(withGrace: Boolean): Boolean =
    if isCorrespondence then outoftimeCorrespondence else outoftimeClock(withGrace)

  private def outoftimeClock(withGrace: Boolean): Boolean =
    clock.exists: c =>
      started && playable && {
        c.outOfTime(turnColor, withGrace) || {
          !c.isRunning && c.players.exists(_.elapsed.centis > 0)
        }
      }

  private def outoftimeCorrespondence: Boolean =
    playableCorrespondenceClock.exists(_.outoftime(turnColor))

  def isCorrespondence  = speed == Speed.Correspondence
  def isSpeed(s: Speed) = speed == s

  def isSwitchable = nonAi && (isCorrespondence || isSimul)

  def hasClock = clock.isDefined

  def hasCorrespondenceClock = daysPerTurn.isDefined

  def isUnlimited = !hasClock && !hasCorrespondenceClock

  def isClockRunning = clock.exists(_.isRunning)

  def estimateClockTotalTime = clock.map(_.estimateTotalSeconds)

  def estimateTotalTime =
    estimateClockTotalTime.orElse(correspondenceClock.map(_.estimateTotalTime)).getOrElse(1200)

  def timeForFirstMove: Centis =
    Centis.ofSeconds:
      import Speed.*
      val base =
        if isTournament then
          speed match
            case UltraBullet => 11
            case Bullet      => 16
            case Blitz       => 21
            case Rapid       => 25
            case _           => 30
        else
          speed match
            case UltraBullet => 15
            case Bullet      => 20
            case Blitz       => 25
            case Rapid       => 30
            case _           => 35
      if variant.chess960 then base * 5 / 4
      else base

  def expirable =
    !bothPlayersHaveMoved &&
      source.exists(Source.expirable.contains) &&
      playable &&
      nonAi &&
      clock.exists(!_.isRunning)

  def timeBeforeExpiration: Option[Centis] = expirable.option:
    Centis.ofMillis(movedAt.toMillis - nowMillis + timeForFirstMove.millis).nonNeg

  def playerWhoDidNotMove: Option[Player] = {
    if playedTurns == Ply.initial then player(startColor).some
    else if playedTurns == Ply.initial.next then player(!startColor).some
    else none
  }.filterNot { player => winnerColor contains player.color }

  def onePlayerHasMoved    = playedTurns > 0
  def bothPlayersHaveMoved = playedTurns > 1

  def startColor = startedAtPly.turn

  def playerMoves(color: Color): Int =
    if color == startColor
    then (playedTurns.value + 1) / 2
    else playedTurns.value / 2

  def playerHasMoved(color: Color) = playerMoves(color) > 0

  def isBeingPlayed = !isPgnImport && !finishedOrAborted

  def olderThan(seconds: Int) = movedAt.isBefore(nowInstant.minusSeconds(seconds))

  def justCreated = createdAt.isAfter(nowInstant.minusSeconds(1))

  def forecastable = started && playable && isCorrespondence && !hasAi

  def userIds: List[UserId] = players.flatMap(_.userId)

  def twoUserIds: Option[(UserId, UserId)] = for
    w <- whitePlayer.userId
    b <- blackPlayer.userId
    if w != b
  yield w -> b

  def userRatings = players.flatMap(_.rating)

  def averageUsersRating = userRatings match
    case a :: b :: Nil => Some((a + b).value / 2)
    case a :: Nil      => Some((a + 1500).value / 2)
    case _             => None

  def withId(newId: GameId) = copy(id = newId)

  def isPgnImport = pgnImport.isDefined

  def resetTurns = copy(chess = chess.copy(ply = Ply.initial, startedAtPly = Ply.initial))

  lazy val opening: Option[Opening.AtPly] =
    if !fromPosition && Variant.list.openingSensibleVariants(variant)
    then OpeningDb.search(sans)
    else none

  def pov(c: Color)                                    = Pov(this, c)
  def playerIdPov(playerId: GamePlayerId): Option[Pov] = player(playerId).map(p => pov(p.color))
  def whitePov                                         = pov(White)
  def blackPov                                         = pov(Black)
  def playerPov(p: Player)                             = pov(p.color)
  def loserPov                                         = loser.map(playerPov)
  def povs: ByColor[Pov]                               = ByColor(pov)

  def setAnalysed = copy(metadata = metadata.copy(analysed = true))

  def secondsSinceCreation = (nowSeconds - createdAt.toSeconds).toInt

  override def toString = s"""Game($id)"""
end Game

def allowRated(variant: Variant, clock: Option[Clock.Config]) =
  variant.standard || clock.exists: c =>
    c.estimateTotalTime >= Centis(3000) &&
      c.limitSeconds > 0 || c.incrementSeconds > 1
