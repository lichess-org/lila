package lila.core
package game

import _root_.chess.Color.{ Black, White }
import _root_.chess.format.Uci
import _root_.chess.format.pgn.SanStr
import _root_.chess.opening.{ Opening, OpeningDb }
import _root_.chess.variant.{ FromPosition, Standard, Variant }
import _root_.chess.{
  ByColor,
  Centis,
  Clock,
  Color,
  CorrespondenceClock,
  Game as ChessGame,
  Mode,
  Ply,
  Speed,
  Status,
  Outcome,
  IntRating
}
import scalalib.model.Days

import lila.core.id.{ GameFullId, GameId, GamePlayerId }
import lila.core.perf.PerfKey
import lila.core.user.User
import lila.core.userId.{ UserId, UserIdOf }

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

  export chess.{ situation, ply, clock, sans, startedAtPly, player as turnColor, history, board, variant }
  export metadata.{ tournamentId, simulId, swissId, drawOffers, source, pgnImport, hasRule }
  export players.{ white as whitePlayer, black as blackPlayer, apply as player }

  lazy val clockHistory = chess.clock.flatMap(loadClockHistory)

  def player[U: UserIdOf](user: U): Option[Player]     = players.find(_.isUser(user))
  def opponentOf[U: UserIdOf](user: U): Option[Player] = player(user).map(opponent)

  def player: Player                                     = players(turnColor)
  def playerById(playerId: GamePlayerId): Option[Player] = players.find(_.id == playerId)

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

  def playedTurns: Ply = ply - startedAtPly

  def flagged = (status == Status.Outoftime).option(turnColor)

  def fullIdOf(player: Player): Option[GameFullId] =
    players.contains(player).option(GameFullId(s"$id${player.id}"))

  def fullIdOf(color: Color) = GameFullId(s"$id${player(color).id}")

  def fullIds: ByColor[GameFullId] = ByColor(fullIdOf)

  export tournamentId.isDefined as isTournament
  export simulId.isDefined as isSimul
  export swissId.isDefined as isSwiss
  def isMandatory          = isTournament || isSimul || isSwiss
  def nonMandatory         = !isMandatory
  def canTakebackOrAddTime = !isMandatory

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
      case d: Uci.Drop => d.square.key * 2
      case m: Uci.Move => m.keys

  def updatePlayer(color: Color, f: Player => Player) =
    copy(players = players.update(color, f))

  def start =
    if started then this
    else
      copy(
        status = Status.Started,
        mode = Mode(mode.rated && userIds.distinct.size == 2)
      )

  def correspondenceClock: Option[CorrespondenceClock] =
    daysPerTurn.map(d => CorrespondenceClock(d.value, turnColor, movedAt))

  def playableCorrespondenceClock: Option[CorrespondenceClock] =
    if playable then correspondenceClock else none

  def perfKey: PerfKey = PerfKey(variant, speed)

  def ratingVariant: Variant =
    if isTournament && variant.fromPosition then Standard else variant

  def started = status >= Status.Started

  def aborted = status == Status.Aborted

  def abort = copy(status = Status.Aborted)

  def playable = status < Status.Aborted && !sourceIs(_.Import)

  def aiLevel: Option[Int] = players.find(_.aiLevel)

  def hasAi: Boolean = players.exists(_.isAi)
  def nonAi          = !hasAi

  def synthetic = id == GameId("synthetic")

  def aiPov: Option[Pov] = players.findColor(_.isAi).map(pov)

  def swissPreventsDraw = isSwiss && playedTurns < 60
  def rulePreventsDraw  = hasRule(_.noEarlyDraw) && playedTurns < 60

  def boosted = rated && finished && bothPlayersHaveMoved && playedTurns < 10

  def abortable       = status == Status.Started && playedTurns < 2 && nonMandatory
  def abortableByUser = abortable && !hasRule(_.noAbort)

  def berserkable =
    isTournament && clock.exists(_.config.berserkable) && status == Status.Started && playedTurns < 2

  def resignable = playable && !abortable
  def forceResignable =
    resignable && nonAi && hasClock && !isSwiss && !hasRule(_.noClaimWin)
  def forceResignableNow = forceResignable && bothPlayersHaveMoved
  def drawable           = playable && !abortable && !swissPreventsDraw && !rulePreventsDraw

  export mode.{ rated, casual }

  def finished = status >= Status.Mate

  def finishedOrAborted = finished || aborted

  def replayable = isPgnImport || finished || (aborted && bothPlayersHaveMoved)

  def fromPosition = variant.fromPosition || source.has(Source.Position)

  def sourceIs(f: Source.type => Source): Boolean = source contains f(Source)
  def lobbyOrPool                                 = source.exists(s => s == Source.Lobby || s == Source.Pool)

  def winner: Option[Player] = players.find(_.isWinner | false)

  def loser: Option[Player] = winner.map(opponent)

  def winnerColor: Option[Color] = winner.map(_.color)
  def outcome: Option[Outcome]   = finished.option(Outcome(winnerColor))

  def winnerUserId: Option[UserId] = winner.flatMap(_.userId)

  def loserUserId: Option[UserId] = loser.flatMap(_.userId)

  def wonBy(c: Color): Option[Boolean] = winner.map(_.color == c)

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

  def hasClock    = clock.isDefined
  def clockConfig = clock.map(_.config)
  def speed       = Speed(clockConfig)

  def hasCorrespondenceClock = daysPerTurn.isDefined

  def isUnlimited = !hasClock && !hasCorrespondenceClock

  def playerWhoDidNotMove: Option[Player] = {
    if playedTurns == Ply.initial then player(startColor).some
    else if playedTurns == Ply.initial.next then player(!startColor).some
    else none
  }.filterNot(winner.contains)

  def bothPlayersHaveMoved = playedTurns > 1

  def startColor = startedAtPly.turn

  def playerMoves(color: Color): Int =
    if color == startColor
    then (playedTurns.value + 1) / 2
    else playedTurns.value / 2

  def playerHasMoved(color: Color) = playerMoves(color) > 0

  def isBeingPlayed = !isPgnImport && !finishedOrAborted

  def forecastable = started && playable && isCorrespondence && !hasAi

  def userIds: List[UserId] = players.flatMap(_.userId)

  def twoUserIds: Option[(UserId, UserId)] = for
    w <- whitePlayer.userId
    b <- blackPlayer.userId
    if w != b
  yield w -> b

  def averageUsersRating: Option[IntRating] = players.flatMap(_.rating) match
    case a :: b :: Nil => Some((a + b).map(_ / 2))
    case a :: Nil      => Some((a + IntRating(1500)).map(_ / 2))
    case _             => None

  def isPgnImport = pgnImport.isDefined

  def hasFewerMovesThanExpected = playedTurns <= reasonableMinimumNumberOfMoves(variant)

  lazy val opening: Option[Opening.AtPly] =
    if !fromPosition && Variant.list.openingSensibleVariants(variant)
    then OpeningDb.search(sans)
    else none

  def pov(c: Color)      = Pov(this, c)
  def povs: ByColor[Pov] = ByColor(pov)

  override def toString = s"""Game($id)"""
end Game
