package lila.game

import chess.Color.{ Black, White }
import chess.format.{ Fen, Uci }
import chess.format.pgn.SanStr
import chess.opening.{ Opening, OpeningDb }
import chess.variant.{ FromPosition, Standard, Variant }
import chess.{
  Ply,
  Castles,
  Centis,
  CheckCount,
  Clock,
  Color,
  Game as ChessGame,
  Mode,
  MoveOrDrop,
  Speed,
  Status
}

import chess.MoveOrDrop.fold

import lila.common.{ Days, Sequence }
import lila.db.ByteArray
import lila.rating.{ Perf, PerfType }
import lila.rating.PerfType.Classical
import lila.user.User

case class Game(
    id: GameId,
    whitePlayer: Player,
    blackPlayer: Player,
    chess: ChessGame,
    loadClockHistory: Clock => Option[ClockHistory] = _ => Game.someEmptyClockHistory,
    status: Status,
    daysPerTurn: Option[Days],
    binaryMoveTimes: Option[ByteArray] = None,
    mode: Mode = Mode.default,
    bookmarks: Int = 0,
    createdAt: Instant = nowInstant,
    movedAt: Instant = nowInstant,
    metadata: Metadata
):

  export chess.{ situation, ply, clock, sans, startedAtPly, player as turnColor }
  export chess.situation.board
  export chess.situation.board.{ history, variant }

  lazy val clockHistory = chess.clock flatMap loadClockHistory

  def players = List(whitePlayer, blackPlayer)

  def player(color: Color): Player = color.fold(whitePlayer, blackPlayer)

  def player(playerId: GamePlayerId): Option[Player] =
    players.find(_.id == playerId)

  def player(user: User): Option[Player] =
    players.find(_ isUser user)

  def player(c: Color.type => Color): Player = player(c(Color))

  def player: Player = player(turnColor)

  def playerByUserId(userId: UserId): Option[Player]   = players.find(_.userId contains userId)
  def opponentByUserId(userId: UserId): Option[Player] = playerByUserId(userId) map opponent

  def hasUserIds(userId1: UserId, userId2: UserId) =
    playerByUserId(userId1).isDefined && playerByUserId(userId2).isDefined

  def hasUserId(userId: UserId) = playerByUserId(userId).isDefined

  def opponent(p: Player): Player = opponent(p.color)

  def opponent(c: Color): Player = player(!c)

  lazy val naturalOrientation =
    if (variant.racingKings) White else Color.fromWhite(whitePlayer before blackPlayer)

  def turnOf(p: Player): Boolean = p == player
  def turnOf(c: Color): Boolean  = c == turnColor
  def turnOf(u: User): Boolean   = player(u) ?? turnOf

  def playedTurns = ply - startedAtPly

  def flagged = (status == Status.Outoftime).option(turnColor)

  def fullIdOf(player: Player): Option[GameFullId] =
    (players contains player) option GameFullId(s"$id${player.id}")

  def fullIdOf(color: Color) = GameFullId(s"$id${player(color).id}")

  def tournamentId = metadata.tournamentId
  def simulId      = metadata.simulId
  def swissId      = metadata.swissId

  def isTournament         = tournamentId.isDefined
  def isSimul              = simulId.isDefined
  def isSwiss              = swissId.isDefined
  def isMandatory          = isTournament || isSimul || isSwiss
  def nonMandatory         = !isMandatory
  def canTakebackOrAddTime = !isMandatory
  def isClassical          = perfType contains Classical

  def hasChat = !isTournament && !isSimul && nonAi

  // we can't rely on the clock,
  // because if moretime was given,
  // elapsed time is no longer representing the game duration
  def durationSeconds: Option[Int] =
    movedAt.toSeconds - createdAt.toSeconds match
      case seconds if seconds > 60 * 60 * 12 => none // no way it lasted more than 12 hours, come on.
      case seconds                           => seconds.toInt.some

  private def everyOther[A](l: List[A]): List[A] =
    l match
      case a :: _ :: tail => a :: everyOther(tail)
      case _              => l

  def moveTimes(color: Color): Option[List[Centis]] = {
    for
      clk <- clock
      inc = clk.incrementOf(color)
      history <- clockHistory
      clocks = history(color)
    yield Centis(0) :: {
      val pairs = clocks.iterator zip clocks.iterator.drop(1)

      // We need to determine if this color's last clock had inc applied.
      // if finished and history.size == playedTurns then game was ended
      // by a players move, such as with mate or autodraw. In this case,
      // the last move of the game, and the only one without inc, is the
      // last entry of the clock history for !turnColor.
      //
      // On the other hand, if history.size is more than playedTurns,
      // then the game ended during a players turn by async event, and
      // the last recorded time is in the history for turnColor.
      val noLastInc = finished && (playedTurns >= history.size) == (color != turnColor)

      pairs map { (first, second) =>
        {
          val d = first - second
          if (pairs.hasNext || !noLastInc) d + inc else d
        }.nonNeg
      } toList
    }
  } orElse binaryMoveTimes.map { binary =>
    // TODO: make movetime.read return List after writes are disabled.
    val base = BinaryFormat.moveTime.read(binary, playedTurns)
    val mts  = if (color == startColor) base else base.drop(1)
    everyOther(mts.toList)
  }

  def moveTimes: Option[Vector[Centis]] = for
    a <- moveTimes(startColor)
    b <- moveTimes(!startColor)
  yield Sequence.interleave(a, b)

  def bothClockStates: Option[Vector[Centis]] = clockHistory.map(_ bothClockStates startColor)

  def sansOf(color: Color): Vector[SanStr] =
    val pivot = if (color == startColor) 0 else 1
    sans.zipWithIndex.collect {
      case (e, i) if (i % 2) == pivot => e
    }

  // apply a move
  def update(
      game: ChessGame, // new chess.Position
      moveOrDrop: MoveOrDrop,
      blur: Boolean = false
  ): Progress =

    def copyPlayer(player: Player) =
      if (blur && moveOrDrop.fold(_.color, _.color) == player.color)
        player.copy(
          blurs = player.blurs.add(playerMoves(player.color))
        )
      else player

    // This must be computed eagerly
    // because it depends on the current time
    val newClockHistory = for {
      clk <- game.clock
      ch  <- clockHistory
    } yield ch.record(turnColor, clk)

    val updated = copy(
      whitePlayer = copyPlayer(whitePlayer),
      blackPlayer = copyPlayer(blackPlayer),
      chess = game,
      binaryMoveTimes = (!isPgnImport && chess.clock.isEmpty).option {
        BinaryFormat.moveTime.write {
          binaryMoveTimes.?? { t =>
            BinaryFormat.moveTime.read(t, playedTurns)
          } :+ Centis.ofLong(nowCentis - movedAt.toCentis).nonNeg
        }
      },
      loadClockHistory = _ => newClockHistory,
      status = game.situation.status | status,
      movedAt = nowInstant
    )

    val state = Event.State(
      turns = game.ply,
      status = (status != updated.status) option updated.status,
      winner = game.situation.winner,
      whiteOffersDraw = whitePlayer.isOfferingDraw,
      blackOffersDraw = blackPlayer.isOfferingDraw
    )

    val clockEvent = updated.chess.clock map Event.Clock.apply orElse {
      updated.playableCorrespondenceClock map Event.CorrespondenceClock.apply
    }

    val events = moveOrDrop.fold(
      Event.Move(_, game.situation, state, clockEvent, updated.board.crazyData),
      Event.Drop(_, game.situation, state, clockEvent, updated.board.crazyData)
    ) :: {
      // abstraction leak, I know.
      (updated.board.variant.threeCheck && game.situation.check.yes) ?? List(
        Event.CheckCount(
          white = updated.history.checkCount.white,
          black = updated.history.checkCount.black
        )
      )
    }

    Progress(this, updated, events)

  def lastMoveKeys: Option[String] =
    history.lastMove map {
      case Uci.Drop(_, target) => s"${target.key}${target.key}"
      case m: Uci.Move         => m.keys
    }

  def updatePlayer(color: Color, f: Player => Player) =
    color.fold(
      copy(whitePlayer = f(whitePlayer)),
      copy(blackPlayer = f(blackPlayer))
    )

  def updatePlayers(f: Player => Player) =
    copy(
      whitePlayer = f(whitePlayer),
      blackPlayer = f(blackPlayer)
    )

  def start =
    if (started) this
    else
      copy(
        status = Status.Started,
        mode = Mode(mode.rated && userIds.distinct.size == 2)
      )

  def startClock =
    clock map { c =>
      start.withClock(c.start)
    }

  def correspondenceClock: Option[CorrespondenceClock] =
    daysPerTurn map { days =>
      val increment   = days.value * 24 * 60 * 60
      val secondsLeft = (movedAt.toSeconds + increment - nowSeconds).toInt max 0
      CorrespondenceClock(
        increment = increment,
        whiteTime = turnColor.fold(secondsLeft, increment).toFloat,
        blackTime = turnColor.fold(increment, secondsLeft).toFloat
      )
    }

  def playableCorrespondenceClock: Option[CorrespondenceClock] =
    playable ?? correspondenceClock

  def speed = Speed(chess.clock.map(_.config))

  def perfKey: Perf.Key = PerfPicker.key(this)
  def perfType          = PerfType(perfKey)

  def started = status >= Status.Started

  def notStarted = !started

  def aborted = status == Status.Aborted

  def playedThenAborted = aborted && bothPlayersHaveMoved

  def abort = copy(status = Status.Aborted)

  def playable = status < Status.Aborted && !imported

  def playableEvenImported = status < Status.Aborted

  def playableBy(p: Player): Boolean = playable && turnOf(p)

  def playableBy(c: Color): Boolean = playableBy(player(c))

  def playableByAi: Boolean = playable && player.isAi

  def mobilePushable = isCorrespondence && playable && nonAi

  def alarmable = hasCorrespondenceClock && playable && nonAi

  def continuable = status != Status.Mate && status != Status.Stalemate

  def aiLevel: Option[Int] = players find (_.isAi) flatMap (_.aiLevel)

  def hasAi: Boolean = players.exists(_.isAi)
  def nonAi          = !hasAi

  def aiPov: Option[Pov] = players.find(_.isAi).map(_.color) map pov

  def mapPlayers(f: Player => Player) =
    copy(
      whitePlayer = f(whitePlayer),
      blackPlayer = f(blackPlayer)
    )

  def drawOffers = metadata.drawOffers

  def playerCanOfferDraw(color: Color) =
    started && playable &&
      ply >= 2 &&
      !player(color).isOfferingDraw &&
      !opponent(color).isAi &&
      !playerHasOfferedDrawRecently(color) &&
      !swissPreventsDraw

  def swissPreventsDraw = isSwiss && playedTurns < 60

  def playerHasOfferedDrawRecently(color: Color) =
    drawOffers.lastBy(color) ?? (_ >= ply - 20)

  def offerDraw(color: Color) = copy(
    metadata = metadata.copy(drawOffers = drawOffers.add(color, ply))
  ).updatePlayer(color, _.offerDraw)

  def playerCouldRematch =
    finishedOrAborted &&
      nonMandatory &&
      !metadata.hasRule(_.NoRematch) &&
      !boosted &&
      !(hasAi && variant == FromPosition && clock.exists(_.config.limitSeconds < 60))

  def playerCanProposeTakeback(color: Color) =
    started && playable && !isTournament && !isSimul &&
      bothPlayersHaveMoved &&
      !player(color).isProposingTakeback &&
      !opponent(color).isProposingTakeback

  def boosted = rated && finished && bothPlayersHaveMoved && playedTurns < 10

  def moretimeable(color: Color) =
    playable && canTakebackOrAddTime && !metadata.hasRule(_.NoGiveTime) && {
      clock.??(_ moretimeable color) || correspondenceClock.??(_ moretimeable color)
    }

  def abortable       = status == Status.Started && playedTurns < 2 && nonMandatory
  def abortableByUser = abortable && !metadata.hasRule(_.NoAbort)

  def berserkable =
    isTournament && clock.??(_.config.berserkable) && status == Status.Started && playedTurns < 2

  def goBerserk(color: Color): Option[Progress] =
    clock.ifTrue(berserkable && !player(color).berserk).map { c =>
      val newClock = c goBerserk color
      Progress(
        this,
        copy(
          chess = chess.copy(clock = Some(newClock)),
          loadClockHistory = _ =>
            clockHistory.map(history => {
              if (history(color).isEmpty) history
              else history.reset(color).record(color, newClock)
            })
        ).updatePlayer(color, _.goBerserk)
      ) ++
        List(
          Event.ClockInc(color, -c.config.berserkPenalty),
          Event.Clock(newClock), // BC
          Event.Berserk(color)
        )
    }

  def resignable = playable && !abortable
  def forceResignable =
    resignable && nonAi && !fromFriend && hasClock && !isSwiss && !metadata.hasRule(_.NoClaimWin)
  def drawable      = playable && !abortable && !swissPreventsDraw
  def forceDrawable = playable && !abortable && !metadata.hasRule(_.NoClaimWin)

  def finish(status: Status, winner: Option[Color]): Game =
    copy(
      status = status,
      whitePlayer = whitePlayer.finish(winner contains White),
      blackPlayer = blackPlayer.finish(winner contains Black),
      chess = chess.copy(clock = clock map { _.stop }),
      loadClockHistory = clk =>
        clockHistory map { history =>
          // If not already finished, we're ending due to an event
          // in the middle of a turn, such as resignation or draw
          // acceptance. In these cases, record a final clock time
          // for the active color. This ensures the end time in
          // clockHistory always matches the final clock time on
          // the board.
          if (!finished) history.record(turnColor, clk)
          else history
        }
    )

  def rated  = mode.rated
  def casual = !rated

  def finished = status >= Status.Mate

  def finishedOrAborted = finished || aborted

  def accountable = playedTurns >= 2 || isTournament

  def replayable = isPgnImport || finished || (aborted && bothPlayersHaveMoved)

  def analysable =
    replayable && playedTurns > 4 &&
      Game.analysableVariants(variant) &&
      !Game.isOldHorde(this)

  def ratingVariant =
    if (isTournament && variant.fromPosition) Standard
    else variant

  def fromPosition = variant.fromPosition || source.??(Source.Position ==)

  def imported = source contains Source.Import

  def fromPool   = source contains Source.Pool
  def fromLobby  = source contains Source.Lobby
  def fromFriend = source contains Source.Friend
  def fromApi    = source contains Source.Api

  def winner = players find (_.wins)

  def loser = winner map opponent

  def winnerColor: Option[Color] = winner map (_.color)

  def winnerUserId: Option[UserId] = winner flatMap (_.userId)

  def loserUserId: Option[UserId] = loser flatMap (_.userId)

  def wonBy(c: Color): Option[Boolean] = winner map (_.color == c)

  def lostBy(c: Color): Option[Boolean] = winner map (_.color != c)

  def drawn = finished && winner.isEmpty

  def outoftime(withGrace: Boolean): Boolean =
    if (isCorrespondence) outoftimeCorrespondence else outoftimeClock(withGrace)

  private def outoftimeClock(withGrace: Boolean): Boolean =
    clock ?? { c =>
      started && playable && {
        c.outOfTime(turnColor, withGrace) || {
          !c.isRunning && c.players.exists(_.elapsed.centis > 0)
        }
      }
    }

  private def outoftimeCorrespondence: Boolean =
    playableCorrespondenceClock ?? { _ outoftime turnColor }

  def isCorrespondence = speed == Speed.Correspondence

  def isSwitchable = nonAi && (isCorrespondence || isSimul)

  def hasClock = clock.isDefined

  def hasCorrespondenceClock = daysPerTurn.isDefined

  def isUnlimited = !hasClock && !hasCorrespondenceClock

  def isClockRunning = clock ?? (_.isRunning)

  def withClock(c: Clock) = Progress(this, copy(chess = chess.copy(clock = Some(c))))

  def correspondenceGiveTime = Progress(this, copy(movedAt = nowInstant))

  def estimateClockTotalTime = clock.map(_.estimateTotalSeconds)

  def estimateTotalTime =
    estimateClockTotalTime orElse
      correspondenceClock.map(_.estimateTotalTime) getOrElse 1200

  def timeForFirstMove: Centis =
    Centis ofSeconds {
      import Speed.*
      val base = if (isTournament) speed match
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
      if (variant.chess960) base * 5 / 4
      else base
    }

  def expirable =
    !bothPlayersHaveMoved && source.exists(Source.expirable.contains) && playable && nonAi && clock.exists(
      !_.isRunning
    )

  def timeBeforeExpiration: Option[Centis] =
    expirable option {
      Centis.ofMillis(movedAt.toMillis - nowMillis + timeForFirstMove.millis).nonNeg
    }

  def playerWhoDidNotMove: Option[Player] = {
    if playedTurns == Ply(0) then player(startColor).some
    else if playedTurns == Ply(1) then player(!startColor).some
    else none
  } filterNot { player => winnerColor contains player.color }

  def onePlayerHasMoved    = playedTurns > 0
  def bothPlayersHaveMoved = playedTurns > 1

  def startColor = startedAtPly.color

  def playerMoves(color: Color): Int =
    if (color == startColor) (playedTurns.value + 1) / 2
    else playedTurns.value / 2

  def playerHasMoved(color: Color) = playerMoves(color) > 0

  def playerBlurPercent(color: Color): Int =
    if (playedTurns > 5) (player(color).blurs.nb * 100) / playerMoves(color)
    else 0

  def isBeingPlayed = !isPgnImport && !finishedOrAborted

  def olderThan(seconds: Int) = movedAt isBefore nowInstant.minusSeconds(seconds)

  def justCreated = createdAt isAfter nowInstant.minusSeconds(1)

  def unplayed = !bothPlayersHaveMoved && (createdAt isBefore Game.unplayedDate)

  def abandoned = (status <= Status.Started) && (movedAt isBefore Game.abandonedDate)

  def forecastable = started && playable && isCorrespondence && !hasAi

  def hasBookmarks = bookmarks > 0

  def showBookmarks = hasBookmarks ?? bookmarks.toString

  def incBookmarks(value: Int) = copy(bookmarks = bookmarks + value)

  def userIds = playerMaps[UserId](_.userId)

  def twoUserIds: Option[(UserId, UserId)] =
    for {
      w <- whitePlayer.userId
      b <- blackPlayer.userId
      if w != b
    } yield w -> b

  def userRatings = playerMaps[IntRating](_.rating)

  def averageUsersRating =
    userRatings match
      case a :: b :: Nil => Some((a + b).value / 2)
      case a :: Nil      => Some((a + 1500).value / 2)
      case _             => None

  def withTournamentId(id: TourId) = copy(metadata = metadata.copy(tournamentId = id.some))
  def withSwissId(id: SwissId)     = copy(metadata = metadata.copy(swissId = id.some))
  def withSimulId(id: SimulId)     = copy(metadata = metadata.copy(simulId = id.some))

  def withId(newId: GameId) = copy(id = newId)

  def source = metadata.source

  def pgnImport   = metadata.pgnImport
  def isPgnImport = pgnImport.isDefined

  def resetTurns = copy(chess = chess.copy(ply = Ply(0), startedAtPly = Ply(0)))

  lazy val opening: Option[Opening.AtPly] =
    (!fromPosition && Variant.list.openingSensibleVariants(variant)) ?? OpeningDb.search(sans)

  def synthetic = id == Game.syntheticId

  private def playerMaps[A](f: Player => Option[A]): List[A] = players flatMap f

  def pov(c: Color)                                    = Pov(this, c)
  def playerIdPov(playerId: GamePlayerId): Option[Pov] = player(playerId) map { Pov(this, _) }
  def whitePov                                         = pov(White)
  def blackPov                                         = pov(Black)
  def playerPov(p: Player)                             = pov(p.color)
  def loserPov                                         = loser map playerPov

  def setAnalysed = copy(metadata = metadata.copy(analysed = true))

  def secondsSinceCreation = (nowSeconds - createdAt.toSeconds).toInt

  def drawReason =
    if (variant.isInsufficientMaterial(board)) DrawReason.InsufficientMaterial.some
    else if (variant.fiftyMoves(history)) DrawReason.FiftyMoves.some
    else if (history.threefoldRepetition) DrawReason.ThreefoldRepetition.some
    else if (drawOffers.normalizedPlies.exists(ply <= _)) DrawReason.MutualAgreement.some
    else None

  override def toString = s"""Game($id)"""

object Game:

  def gameId(fullId: GameFullId)                     = GameId(fullId.value take gameIdSize)
  def playerId(fullId: GameFullId)                   = GamePlayerId(fullId.value drop gameIdSize)
  def fullId(gameId: GameId, playerId: GamePlayerId) = GameFullId(s"$gameId$playerId")

  case class OnStart(id: GameId)
  case class WithInitialFen(game: Game, fen: Option[Fen.Epd])

  case class SideAndStart(color: Color, startedAtPly: Ply):
    def startColor = startedAtPly.color

  val syntheticId = GameId("synthetic")

  val maxPlaying         = 200 // including correspondence
  val maxPlayingRealtime = 100

  val maxPlies = Ply(600) // unlimited can cause StackOverflowError

  val analysableVariants: Set[Variant] = Set(
    chess.variant.Standard,
    chess.variant.Crazyhouse,
    chess.variant.Chess960,
    chess.variant.KingOfTheHill,
    chess.variant.ThreeCheck,
    chess.variant.Antichess,
    chess.variant.FromPosition,
    chess.variant.Horde,
    chess.variant.Atomic,
    chess.variant.RacingKings
  )

  val unanalysableVariants: Set[Variant] = Variant.list.all.toSet -- analysableVariants

  val variantsWhereWhiteIsBetter: Set[Variant] = Set(
    chess.variant.ThreeCheck,
    chess.variant.Atomic,
    chess.variant.Horde,
    chess.variant.RacingKings,
    chess.variant.Antichess
  )

  val blindModeVariants: Set[Variant] = Set(
    chess.variant.Standard,
    chess.variant.Chess960,
    chess.variant.KingOfTheHill,
    chess.variant.ThreeCheck,
    chess.variant.FromPosition,
    chess.variant.Antichess,
    chess.variant.Atomic,
    chess.variant.RacingKings,
    chess.variant.Horde
  )

  val hordeWhitePawnsSince = instantOf(2015, 4, 11, 10, 0)

  def isOldHorde(game: Game) =
    game.variant == chess.variant.Horde &&
      game.createdAt.isBefore(Game.hordeWhitePawnsSince)

  def allowRated(variant: Variant, clock: Option[Clock.Config]) =
    variant.standard || {
      clock ?? { c =>
        c.estimateTotalTime >= Centis(3000) &&
        c.limitSeconds > 0 || c.incrementSeconds > 1
      }
    }

  val gameIdSize   = 8
  val playerIdSize = 4
  val fullIdSize   = 12
  val tokenSize    = 4

  val unplayedHours = 24
  def unplayedDate  = nowInstant minusHours unplayedHours

  val abandonedDays = Days(21)
  def abandonedDate = nowInstant minusDays abandonedDays.value

  def strToIdOpt(str: String): Option[GameId]        = strToId(str).some.filter(validId)
  inline def strToId(str: String): GameId            = GameId(str take gameIdSize)
  inline def anyToId(anyId: GameAnyId): GameId       = strToId(anyId.value)
  inline def fullToId(fullId: GameFullId): GameId    = strToId(fullId.value)
  def takePlayerId(fullId: GameFullId): GamePlayerId = GamePlayerId(fullId.value drop gameIdSize)

  private val idRegex     = """[\w-]{8}""".r
  def validId(id: GameId) = idRegex matches id.value

  def isBoardCompatible(game: Game): Boolean =
    game.clock.fold(true) { c =>
      isBoardCompatible(c.config) || {
        (game.hasAi || game.fromFriend) && chess.Speed(c.config) >= Speed.Blitz
      }
    }

  def isBoardCompatible(clock: Clock.Config): Boolean =
    chess.Speed(clock) >= Speed.Rapid

  def isBotCompatible(game: Game): Boolean = {
    game.hasAi || game.fromFriend || game.fromApi
  } && isBotCompatible(game.speed)

  def isBotCompatible(speed: Speed): Boolean = speed >= Speed.Bullet

  def isBoardOrBotCompatible(game: Game) = isBoardCompatible(game) || isBotCompatible(game)

  private[game] val emptyCheckCount = CheckCount(0, 0)

  private[game] val someEmptyClockHistory = Some(ClockHistory())

  def make(
      chess: ChessGame,
      whitePlayer: Player,
      blackPlayer: Player,
      mode: Mode,
      source: Source,
      pgnImport: Option[PgnImport],
      daysPerTurn: Option[Days] = None,
      rules: Set[GameRule] = Set.empty
  ): NewGame =
    val createdAt = nowInstant
    NewGame(
      Game(
        id = IdGenerator.uncheckedGame,
        whitePlayer = whitePlayer,
        blackPlayer = blackPlayer,
        chess = chess,
        status = Status.Created,
        daysPerTurn = daysPerTurn,
        mode = mode,
        metadata = metadata(source).copy(pgnImport = pgnImport, rules = rules),
        createdAt = createdAt,
        movedAt = createdAt
      )
    )

  def metadata(source: Source) = Metadata.empty.copy(source = source.some)

  object BSONFields:

    val id                = "_id"
    val whitePlayer       = "p0"
    val blackPlayer       = "p1"
    val playerIds         = "is"
    val playerUids        = "us"
    val playingUids       = "pl"
    val binaryPieces      = "ps"
    val oldPgn            = "pg"
    val huffmanPgn        = "hp"
    val status            = "s"
    val turns             = "t"
    val startedAtTurn     = "st"
    val clock             = "c"
    val positionHashes    = "ph"
    val checkCount        = "cc"
    val castleLastMove    = "cl"
    val unmovedRooks      = "ur"
    val daysPerTurn       = "cd"
    val moveTimes         = "mt"
    val whiteClockHistory = "cw"
    val blackClockHistory = "cb"
    val rated             = "ra"
    val analysed          = "an"
    val variant           = "v"
    val crazyData         = "chd"
    val bookmarks         = "bm"
    val createdAt         = "ca"
    val movedAt           = "ua" // ua = updatedAt (bc)
    val source            = "so"
    val pgnImport         = "pgni"
    val tournamentId      = "tid"
    val swissId           = "iid"
    val simulId           = "sid"
    val tvAt              = "tv"
    val winnerColor       = "w"
    val winnerId          = "wid"
    val initialFen        = "if"
    val checkAt           = "ck"
    val perfType          = "pt" // only set on student games for aggregation
    val drawOffers        = "do"
    val rules             = "rules"

case class CastleLastMove(castles: Castles, lastMove: Option[Uci])

object CastleLastMove:

  def init = CastleLastMove(Castles.all, None)

  import reactivemongo.api.bson.*
  import lila.db.dsl.*
  import lila.db.ByteArray.byteArrayHandler

  private[game] given castleLastMoveHandler: BSONHandler[CastleLastMove] = tryHandler[CastleLastMove](
    { case bin: BSONBinary =>
      byteArrayHandler readTry bin map BinaryFormat.castleLastMove.read
    },
    clmt => byteArrayHandler writeTry { BinaryFormat.castleLastMove write clmt } get
  )

case class ClockHistory(
    white: Vector[Centis] = Vector.empty,
    black: Vector[Centis] = Vector.empty
):

  def update(color: Color, f: Vector[Centis] => Vector[Centis]): ClockHistory =
    color.fold(copy(white = f(white)), copy(black = f(black)))

  def record(color: Color, clock: Clock): ClockHistory =
    update(color, _ :+ clock.remainingTime(color))

  def reset(color: Color) = update(color, _ => Vector.empty)

  def apply(color: Color): Vector[Centis] = color.fold(white, black)

  def last(color: Color) = apply(color).lastOption

  def size = white.size + black.size

  // first state is of the color that moved first.
  def bothClockStates(firstMoveBy: Color): Vector[Centis] =
    Sequence.interleave(
      firstMoveBy.fold(white, black),
      firstMoveBy.fold(black, white)
    )

enum DrawReason:
  case MutualAgreement, FiftyMoves, ThreefoldRepetition, InsufficientMaterial
