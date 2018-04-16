package lila.game

import chess.Color.{ White, Black }
import chess.format.{ Uci, FEN }
import chess.opening.{ FullOpening, FullOpeningDB }
import chess.variant.{ Variant, Standard }
import chess.{ Speed, PieceMap, MoveMetrics, History => ChessHistory, CheckCount, Castles, Board, MoveOrDrop, Pos, Game => ChessGame, Clock, Status, Color, Mode, PositionHash, UnmovedRooks, Centis, Situation }
import org.joda.time.DateTime

import lila.common.Sequence
import lila.db.ByteArray
import lila.rating.PerfType
import lila.user.User

case class Game(
    id: Game.ID,
    whitePlayer: Player,
    blackPlayer: Player,
    chess: ChessGame,
    loadClockHistory: Clock => Option[ClockHistory] = _ => Game.someEmptyClockHistory,
    status: Status,
    daysPerTurn: Option[Int],
    binaryMoveTimes: Option[ByteArray] = None,
    mode: Mode = Mode.default,
    next: Option[String] = None,
    bookmarks: Int = 0,
    createdAt: DateTime = DateTime.now,
    movedAt: DateTime = DateTime.now,
    metadata: Metadata
) {
  lazy val clockHistory = chess.clock flatMap loadClockHistory

  def situation = chess.situation
  def board = chess.situation.board
  def history = chess.situation.board.history
  def variant = chess.situation.board.variant
  def turns = chess.turns
  def clock = chess.clock
  def pgnMoves = chess.pgnMoves

  val players = List(whitePlayer, blackPlayer)

  def player(color: Color): Player = color.fold(whitePlayer, blackPlayer)

  def player(playerId: Player.ID): Option[Player] =
    players find (_.id == playerId)

  def player(user: User): Option[Player] =
    players find (_ isUser user)

  def player(c: Color.type => Color): Player = player(c(Color))

  def isPlayerFullId(player: Player, fullId: String): Boolean =
    (fullId.size == Game.fullIdSize) && player.id == (fullId drop 8)

  def player: Player = player(turnColor)

  def playerByUserId(userId: String): Option[Player] = players.find(_.userId contains userId)
  def opponentByUserId(userId: String): Option[Player] = playerByUserId(userId) map opponent

  def opponent(p: Player): Player = opponent(p.color)

  def opponent(c: Color): Player = player(!c)

  lazy val firstColor = Color(whitePlayer before blackPlayer)
  def firstPlayer = player(firstColor)
  def secondPlayer = player(!firstColor)

  def turnColor = Color((turns & 1) == 0)

  def turnOf(p: Player): Boolean = p == player
  def turnOf(c: Color): Boolean = c == turnColor
  def turnOf(u: User): Boolean = player(u) ?? turnOf

  def playedTurns = turns - chess.startedAtTurn

  def flagged = (status == Status.Outoftime).option(turnColor)

  def fullIdOf(player: Player): Option[String] =
    (players contains player) option s"$id${player.id}"

  def fullIdOf(color: Color): String = s"$id${player(color).id}"

  def tournamentId = metadata.tournamentId
  def simulId = metadata.simulId

  def isTournament = tournamentId.isDefined
  def isSimul = simulId.isDefined
  def isMandatory = isTournament || isSimul
  def nonMandatory = !isMandatory

  def hasChat = !isTournament && !isSimul && nonAi

  // we can't rely on the clock,
  // because if moretime was given,
  // elapsed time is no longer representing the game duration
  def durationSeconds: Option[Int] = (movedAt.getSeconds - createdAt.getSeconds) match {
    case seconds if seconds > 60 * 60 * 12 => none // no way it lasted more than 12 hours, come on.
    case seconds => seconds.toInt.some
  }

  def everyOther[A](l: List[A]): List[A] = l match {
    case a :: b :: tail => a :: everyOther(tail)
    case _ => l
  }

  def moveTimes(color: Color): Option[List[Centis]] = {
    for {
      clk <- clock
      inc = clk.incrementOf(color)
      history <- clockHistory
      clocks = history(color)
    } yield Centis(0) :: {
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
      val noLastInc = finished && (history.size <= playedTurns) == (color != turnColor)

      pairs map {
        case (first, second) => {
          val d = first - second
          if (pairs.hasNext || !noLastInc) d + inc else d
        } nonNeg
      } toList
    }
  } orElse binaryMoveTimes.map { binary =>
    // TODO: make movetime.read return List after writes are disabled.
    val base = BinaryFormat.moveTime.read(binary, playedTurns)
    val mts = if (color == startColor) base else base.drop(1)
    everyOther(mts.toList)
  }

  def moveTimes: Option[Vector[Centis]] = for {
    a <- moveTimes(startColor)
    b <- moveTimes(!startColor)
  } yield Sequence.interleave(a, b)

  def bothClockStates: Option[Vector[Centis]] = clockHistory.map(_ bothClockStates startColor)

  def pgnMoves(color: Color): PgnMoves = {
    val pivot = if (color == startColor) 0 else 1
    pgnMoves.zipWithIndex.collect {
      case (e, i) if (i % 2) == pivot => e
    }
  }

  def update(
    game: ChessGame,
    moveOrDrop: MoveOrDrop,
    blur: Boolean = false,
    moveMetrics: MoveMetrics = MoveMetrics()
  ): Progress = {

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
      ch <- clockHistory
    } yield ch.record(turnColor, clk)

    val updated = copy(
      whitePlayer = copyPlayer(whitePlayer),
      blackPlayer = copyPlayer(blackPlayer),
      chess = game,
      binaryMoveTimes = (!isPgnImport && !chess.clock.isDefined).option {
        BinaryFormat.moveTime.write {
          binaryMoveTimes.?? { t =>
            BinaryFormat.moveTime.read(t, playedTurns)
          } :+ Centis(nowCentis - movedAt.getCentis).nonNeg
        }
      },
      loadClockHistory = _ => newClockHistory,
      status = game.situation.status | status,
      movedAt = DateTime.now
    )

    val state = Event.State(
      color = game.situation.color,
      turns = game.turns,
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
    ) ::
      {
        // abstraction leak, I know.
        (updated.board.variant.threeCheck && game.situation.check) ?? List(Event.CheckCount(
          white = updated.history.checkCount.white,
          black = updated.history.checkCount.black
        ))
      }.toList

    Progress(this, updated, events)
  }

  def lastMoveKeys: Option[String] = history.lastMove map {
    case Uci.Drop(target, _) => s"$target$target"
    case m: Uci.Move => m.keys
  }

  def updatePlayer(color: Color, f: Player => Player) = color.fold(
    copy(whitePlayer = f(whitePlayer)),
    copy(blackPlayer = f(blackPlayer))
  )

  def updatePlayers(f: Player => Player) = copy(
    whitePlayer = f(whitePlayer),
    blackPlayer = f(blackPlayer)
  )

  def start = started.fold(this, copy(
    status = Status.Started,
    mode = Mode(mode.rated && userIds.distinct.size == 2)
  ))

  def correspondenceClock: Option[CorrespondenceClock] = daysPerTurn map { days =>
    val increment = days * 24 * 60 * 60
    val secondsLeft = (movedAt.getSeconds + increment - nowSeconds).toInt max 0
    CorrespondenceClock(
      increment = increment,
      whiteTime = turnColor.fold(secondsLeft, increment),
      blackTime = turnColor.fold(increment, secondsLeft)
    )
  }

  def playableCorrespondenceClock: Option[CorrespondenceClock] =
    playable ?? correspondenceClock

  def speed = Speed(chess.clock.map(_.config))

  def perfKey = PerfPicker.key(this)
  def perfType = PerfType(perfKey)

  def started = status >= Status.Started

  def notStarted = !started

  def aborted = status == Status.Aborted

  def playedThenAborted = aborted && bothPlayersHaveMoved

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
  def nonAi = !hasAi

  def aiPov: Option[Pov] = players.find(_.isAi).map(_.color) map pov

  def mapPlayers(f: Player => Player) = copy(
    whitePlayer = f(whitePlayer),
    blackPlayer = f(blackPlayer)
  )

  def playerCanOfferDraw(color: Color) =
    started && playable &&
      turns >= 2 &&
      !player(color).isOfferingDraw &&
      !(opponent(color).isAi) &&
      !(playerHasOfferedDraw(color))

  def playerHasOfferedDraw(color: Color) =
    player(color).lastDrawOffer ?? (_ >= turns - 20)

  def playerCanRematch(color: Color) =
    !player(color).isOfferingRematch &&
      finishedOrAborted &&
      nonMandatory &&
      !boosted

  def playerCanProposeTakeback(color: Color) =
    started && playable && !isTournament && !isSimul &&
      bothPlayersHaveMoved &&
      !player(color).isProposingTakeback &&
      !opponent(color).isProposingTakeback

  def boosted = rated && finished && bothPlayersHaveMoved && playedTurns < 10

  def moretimeable(color: Color) = playable && nonMandatory && {
    clock.??(_ moretimeable color) || correspondenceClock.??(_ moretimeable color)
  }

  def abortable = status == Status.Started && playedTurns < 2 && nonMandatory

  def berserkable = clock.??(_.config.berserkable) && status == Status.Started && playedTurns < 2

  def goBerserk(color: Color) =
    clock.ifTrue(berserkable && !player(color).berserk).map { c =>
      val newClock = c goBerserk color
      Progress(this, copy(
        chess = chess.copy(clock = Some(newClock)),
        loadClockHistory = _ => clockHistory.map(history => {
          if (history(color).isEmpty) history
          else history.reset(color).record(color, newClock)
        })
      ).updatePlayer(color, _.goBerserk)) ++
        List(
          Event.ClockInc(color, -c.config.berserkPenalty),
          Event.Clock(newClock), // BC
          Event.Berserk(color)
        )
    }

  def resignable = playable && !abortable
  def drawable = playable && !abortable

  def finish(status: Status, winner: Option[Color]) = {
    val newClock = clock map { _.stop }
    Progress(
      this,
      copy(
        status = status,
        whitePlayer = whitePlayer.finish(winner contains White),
        blackPlayer = blackPlayer.finish(winner contains Black),
        chess = chess.copy(clock = newClock),
        loadClockHistory = clk => clockHistory map { history =>
          // If not already finished, we're ending due to an event
          // in the middle of a turn, such as resignation or draw
          // acceptance. In these cases, record a final clock time
          // for the active color. This ensures the end time in
          // clockHistory always matches the final clock time on
          // the board.
          if (!finished) history.record(turnColor, clk)
          else history
        }
      ),
      // Events here for BC.
      List(Event.End(winner)) ::: newClock.??(c => List(Event.Clock(c)))
    )
  }

  def rated = mode.rated
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

  def fromPosition = variant.fromPosition || source.??(Source.Position==)

  def imported = source contains Source.Import

  def fromPool = source contains Source.Pool
  def fromLobby = source contains Source.Lobby
  def fromFriend = source contains Source.Friend

  def winner = players find (_.wins)

  def loser = winner map opponent

  def winnerColor: Option[Color] = winner map (_.color)

  def winnerUserId: Option[String] = winner flatMap (_.userId)

  def loserUserId: Option[String] = loser flatMap (_.userId)

  def wonBy(c: Color): Option[Boolean] = winner map (_.color == c)

  def lostBy(c: Color): Option[Boolean] = winner map (_.color != c)

  def drawn = finished && winner.isEmpty

  def outoftime(withGrace: Boolean): Boolean =
    if (isCorrespondence) outoftimeCorrespondence else outoftimeClock(withGrace)

  private def outoftimeClock(withGrace: Boolean): Boolean = clock ?? { c =>
    started && playable && (bothPlayersHaveMoved || isSimul) && {
      (!c.isRunning && !c.isInit) || c.outOfTime(turnColor, withGrace)
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

  def correspondenceGiveTime = Progress(this, copy(movedAt = DateTime.now))

  def estimateClockTotalTime = clock.map(_.estimateTotalSeconds)

  def estimateTotalTime = estimateClockTotalTime orElse
    correspondenceClock.map(_.estimateTotalTime) getOrElse 1200

  def timeForFirstMove: Centis = Centis ofSeconds {
    import Speed._
    val base = if (isTournament) speed match {
      case UltraBullet => 11
      case Bullet => 16
      case Blitz => 21
      case Rapid => 25
      case _ => 30
    }
    else speed match {
      case UltraBullet => 15
      case Bullet => 20
      case Blitz => 25
      case Rapid => 30
      case _ => 35
    }
    if (variant.chess960) (base * 2) atMost 90
    else base
  }

  def expirable =
    source.exists(Source.expirable.contains) && playable && !bothPlayersHaveMoved && nonAi && hasClock

  def timeBeforeExpiration: Option[Centis] = expirable option {
    Centis.ofMillis(movedAt.getMillis - nowMillis + timeForFirstMove.millis).nonNeg
  }

  def playerWhoDidNotMove: Option[Player] = playedTurns match {
    case 0 => player(startColor).some
    case 1 => player(!startColor).some
    case _ => none
  }

  def onePlayerHasMoved = playedTurns > 0
  def bothPlayersHaveMoved = playedTurns > 1

  def startColor = Color(chess.startedAtTurn % 2 == 0)

  def playerMoves(color: Color): Int =
    if (color == startColor) (playedTurns + 1) / 2
    else playedTurns / 2

  def playerHasMoved(color: Color) = playerMoves(color) > 0

  def playerBlurPercent(color: Color): Int =
    if (playedTurns > 5) (player(color).blurs.nb * 100) / playerMoves(color)
    else 0

  def isBeingPlayed = !isPgnImport && !finishedOrAborted

  def olderThan(seconds: Int) = movedAt isBefore DateTime.now.minusSeconds(seconds)

  def justCreated = createdAt isAfter DateTime.now.minusSeconds(1)

  def unplayed = !bothPlayersHaveMoved && (createdAt isBefore Game.unplayedDate)

  def abandoned = (status <= Status.Started) && {
    movedAt isBefore {
      if (hasAi && !hasCorrespondenceClock) Game.aiAbandonedDate
      else Game.abandonedDate
    }
  }

  def forecastable = started && playable && isCorrespondence && !hasAi

  def hasBookmarks = bookmarks > 0

  def showBookmarks = hasBookmarks ?? bookmarks.toString

  def userIds = playerMaps(_.userId)

  def userRatings = playerMaps(_.rating)

  def averageUsersRating = userRatings match {
    case a :: b :: Nil => Some((a + b) / 2)
    case a :: Nil => Some((a + 1500) / 2)
    case _ => None
  }

  def withTournamentId(id: String) = copy(metadata = metadata.copy(tournamentId = id.some))

  def withSimulId(id: String) = copy(metadata = metadata.copy(simulId = id.some))

  def withId(newId: String) = copy(id = newId)

  def source = metadata.source

  def pgnImport = metadata.pgnImport
  def isPgnImport = pgnImport.isDefined

  def resetTurns = copy(
    chess = chess.copy(turns = 0, startedAtTurn = 0)
  )

  lazy val opening: Option[FullOpening.AtPly] =
    if (fromPosition || !Variant.openingSensibleVariants(variant)) none
    else FullOpeningDB search pgnMoves

  def synthetic = id == Game.syntheticId

  private def playerMaps[A](f: Player => Option[A]): List[A] = players flatMap { f(_) }

  def pov(c: Color) = Pov(this, c)
  def playerIdPov(playerId: Player.ID): Option[Pov] = player(playerId) map { Pov(this, _) }
  def whitePov = pov(White)
  def blackPov = pov(Black)
  def playerPov(p: Player) = pov(p.color)
  def loserPov = loser map playerPov
}

object Game {

  type ID = String

  case class WithInitialFen(game: Game, fen: Option[FEN])

  val syntheticId = "synthetic"

  val maxPlayingRealtime = 100 // plus 200 correspondence games

  val maxPlies = 600 // unlimited can cause StackOverflowError

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

  val unanalysableVariants: Set[Variant] = Variant.all.toSet -- analysableVariants

  val variantsWhereWhiteIsBetter: Set[Variant] = Set(
    chess.variant.ThreeCheck,
    chess.variant.Atomic,
    chess.variant.Horde,
    chess.variant.RacingKings,
    chess.variant.Antichess
  )

  val hordeWhitePawnsSince = new DateTime(2015, 4, 11, 10, 0)

  def isOldHorde(game: Game) =
    game.variant == chess.variant.Horde &&
      game.createdAt.isBefore(Game.hordeWhitePawnsSince)

  def allowRated(variant: Variant, clock: Clock.Config) =
    variant.standard || clock.estimateTotalTime >= Centis(3000)

  val gameIdSize = 8
  val playerIdSize = 4
  val fullIdSize = 12
  val tokenSize = 4

  val unplayedHours = 24
  def unplayedDate = DateTime.now minusHours unplayedHours

  val abandonedDays = 21
  def abandonedDate = DateTime.now minusDays abandonedDays

  val aiAbandonedHours = 6
  def aiAbandonedDate = DateTime.now minusHours aiAbandonedHours

  def takeGameId(fullId: String) = fullId take gameIdSize
  def takePlayerId(fullId: String) = fullId drop gameIdSize

  private[game] val emptyCheckCount = CheckCount(0, 0)

  private[game] val someEmptyClockHistory = Some(ClockHistory())

  def make(
    chess: ChessGame,
    whitePlayer: Player,
    blackPlayer: Player,
    mode: Mode,
    source: Source,
    pgnImport: Option[PgnImport],
    daysPerTurn: Option[Int] = None
  ): Game = {
    var createdAt = DateTime.now
    Game(
      id = IdGenerator.game,
      whitePlayer = whitePlayer,
      blackPlayer = blackPlayer,
      chess = chess,
      status = Status.Created,
      daysPerTurn = daysPerTurn,
      mode = mode,
      metadata = Metadata(
        source = source.some,
        pgnImport = pgnImport,
        tournamentId = none,
        simulId = none,
        analysed = false
      ),
      createdAt = createdAt,
      movedAt = createdAt
    )
  }

  object BSONFields {

    val id = "_id"
    val whitePlayer = "p0"
    val blackPlayer = "p1"
    val playerIds = "is"
    val playerUids = "us"
    val playingUids = "pl"
    val binaryPieces = "ps"
    val oldPgn = "pg"
    val huffmanPgn = "hp"
    val status = "s"
    val turns = "t"
    val startedAtTurn = "st"
    val clock = "c"
    val positionHashes = "ph"
    val checkCount = "cc"
    val castleLastMove = "cl"
    val unmovedRooks = "ur"
    val daysPerTurn = "cd"
    val moveTimes = "mt"
    val whiteClockHistory = "cw"
    val blackClockHistory = "cb"
    val rated = "ra"
    val analysed = "an"
    val variant = "v"
    val crazyData = "chd"
    val next = "ne"
    val bookmarks = "bm"
    val createdAt = "ca"
    val movedAt = "ua" // ua = updatedAt (bc)
    val source = "so"
    val pgnImport = "pgni"
    val tournamentId = "tid"
    val simulId = "sid"
    val tvAt = "tv"
    val winnerColor = "w"
    val winnerId = "wid"
    val initialFen = "if"
    val checkAt = "ck"
  }
}

case class CastleLastMove(castles: Castles, lastMove: Option[Uci])

object CastleLastMove {

  def init = CastleLastMove(Castles.all, None)

  import reactivemongo.bson._
  import lila.db.ByteArray.ByteArrayBSONHandler

  private[game] implicit val castleLastMoveBSONHandler = new BSONHandler[BSONBinary, CastleLastMove] {
    def read(bin: BSONBinary) = BinaryFormat.castleLastMove read {
      ByteArrayBSONHandler read bin
    }
    def write(clmt: CastleLastMove) = ByteArrayBSONHandler write {
      BinaryFormat.castleLastMove write clmt
    }
  }
}

case class ClockHistory(
    white: Vector[Centis] = Vector.empty,
    black: Vector[Centis] = Vector.empty
) {

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
}
