package lila.game

import chess.Color.{ White, Black }
import chess.format.{ Uci, FEN }
import chess.opening.{ FullOpening, FullOpeningDB }
import chess.Pos.piotr, chess.Role.forsyth
import chess.variant.{ Variant, Crazyhouse }
import chess.{ History => ChessHistory, CheckCount, Castles, Role, Board, MoveOrDrop, Pos, Game => ChessGame, Clock, Status, Color, Piece, Mode, PositionHash, UnmovedRooks }
import org.joda.time.DateTime
import scala.concurrent.duration.FiniteDuration

import lila.db.ByteArray
import lila.rating.PerfType
import lila.user.User

case class Game(
    id: String,
    whitePlayer: Player,
    blackPlayer: Player,
    binaryPieces: ByteArray,
    binaryPgn: ByteArray,
    status: Status,
    turns: Int, // = ply
    startedAtTurn: Int,
    clock: Option[Clock],
    castleLastMoveTime: CastleLastMoveTime,
    unmovedRooks: UnmovedRooks,
    daysPerTurn: Option[Int],
    positionHashes: PositionHash = Array(),
    checkCount: CheckCount = CheckCount(0, 0),
    binaryMoveTimes: ByteArray = ByteArray.empty, // tenths of seconds
    mode: Mode = Mode.default,
    variant: Variant = Variant.default,
    crazyData: Option[Crazyhouse.Data] = None,
    next: Option[String] = None,
    bookmarks: Int = 0,
    createdAt: DateTime = DateTime.now,
    updatedAt: Option[DateTime] = None,
    metadata: Metadata) {

  val players = List(whitePlayer, blackPlayer)

  def player(color: Color): Player = color match {
    case White => whitePlayer
    case Black => blackPlayer
  }

  def player(playerId: String): Option[Player] =
    players find (_.id == playerId)

  def player(user: User): Option[Player] =
    players find (_ isUser user)

  def player(c: Color.type => Color): Player = player(c(Color))

  def isPlayerFullId(player: Player, fullId: String): Boolean =
    (fullId.size == Game.fullIdSize) && player.id == (fullId drop 8)

  def player: Player = player(turnColor)

  def playerByUserId(userId: String): Option[Player] = players.find(_.userId contains userId)

  def opponent(p: Player): Player = opponent(p.color)

  def opponent(c: Color): Player = player(!c)

  lazy val firstColor = Color(whitePlayer before blackPlayer)
  def firstPlayer = player(firstColor)
  def secondPlayer = player(!firstColor)

  def turnColor = Color(0 == turns % 2)

  def turnOf(p: Player): Boolean = p == player
  def turnOf(c: Color): Boolean = c == turnColor
  def turnOf(u: User): Boolean = player(u) ?? turnOf

  def playedTurns = turns - startedAtTurn

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

  // in tenths
  private def lastMoveTime: Option[Long] = castleLastMoveTime.lastMoveTime map {
    _.toLong + (createdAt.getMillis / 100)
  } orElse updatedAt.map(_.getMillis / 100)

  def lastMoveDateTime: Option[DateTime] = castleLastMoveTime.lastMoveTime map { lmt =>
    createdAt plus (lmt * 100l)
  } orElse updatedAt

  def updatedAtOrCreatedAt = updatedAt | createdAt

  def durationSeconds = (updatedAtOrCreatedAt.getSeconds - createdAt.getSeconds).toInt

  def lastMoveTimeInSeconds: Option[Int] = lastMoveTime.map(x => (x / 10).toInt)

  // in tenths of seconds
  lazy val moveTimes: Vector[Int] = BinaryFormat.moveTime read binaryMoveTimes take playedTurns

  def moveTimes(color: Color): List[Int] = {
    val pivot = if (color == startColor) 0 else 1
    moveTimes.toList.zipWithIndex.collect {
      case (e, i) if (i % 2) == pivot => e
    }
  }

  def moveTimesInSeconds: Vector[Float] = moveTimes.map(_.toFloat / 10)

  lazy val pgnMoves: PgnMoves = BinaryFormat.pgn read binaryPgn

  def openingPgnMoves(nb: Int): PgnMoves = BinaryFormat.pgn.read(binaryPgn, nb)

  def pgnMoves(color: Color): PgnMoves = {
    val pivot = if (color == startColor) 0 else 1
    pgnMoves.zipWithIndex.collect {
      case (e, i) if (i % 2) == pivot => e
    }
  }

  lazy val toChess: ChessGame = {

    val pieces = BinaryFormat.piece.read(binaryPieces, variant)

    ChessGame(
      board = Board(pieces, toChessHistory, variant, crazyData),
      player = Color(0 == turns % 2),
      clock = clock,
      turns = turns,
      startedAtTurn = startedAtTurn,
      pgnMoves = pgnMoves)
  }

  lazy val toChessHistory = ChessHistory(
    lastMove = castleLastMoveTime.lastMove map {
    case (orig, dest) => Uci.Move(orig, dest)
  },
    castles = castleLastMoveTime.castles,
    positionHashes = positionHashes,
    checkCount = checkCount,
    unmovedRooks = unmovedRooks)

  def update(
    game: ChessGame,
    moveOrDrop: MoveOrDrop,
    blur: Boolean = false,
    lag: Option[FiniteDuration] = None): Progress = {
    val (history, situation) = (game.board.history, game.situation)

    def copyPlayer(player: Player) = player.copy(
      blurs = math.min(
        playerMoves(player.color),
        player.blurs + (blur && moveOrDrop.fold(_.color, _.color) == player.color).fold(1, 0))
    )

    val updated = copy(
      whitePlayer = copyPlayer(whitePlayer),
      blackPlayer = copyPlayer(blackPlayer),
      binaryPieces = BinaryFormat.piece write game.board.pieces,
      binaryPgn = BinaryFormat.pgn write game.pgnMoves,
      turns = game.turns,
      positionHashes = history.positionHashes,
      checkCount = history.checkCount,
      crazyData = situation.board.crazyData,
      castleLastMoveTime = CastleLastMoveTime(
        castles = history.castles,
        lastMove = history.lastMove.map(_.origDest),
        lastMoveTime = Some(((nowMillis - createdAt.getMillis) / 100).toInt),
        check = situation.checkSquare),
      unmovedRooks = game.board.unmovedRooks,
      binaryMoveTimes = isPgnImport.fold(
        ByteArray.empty,
        BinaryFormat.moveTime write lastMoveTime.fold(Vector(0)) { lmt =>
          moveTimes :+ {
            (nowTenths - lmt - (lag.??(_.toMillis) / 100)).toInt max 0
          }
        }
      ),
      status = situation.status | status,
      clock = game.clock)

    val state = Event.State(
      color = situation.color,
      turns = game.turns,
      status = (status != updated.status) option updated.status,
      winner = situation.winner,
      whiteOffersDraw = whitePlayer.isOfferingDraw,
      blackOffersDraw = blackPlayer.isOfferingDraw)

    val clockEvent = updated.clock map Event.Clock.apply orElse {
      updated.playableCorrespondenceClock map Event.CorrespondenceClock.apply
    }

    val events = moveOrDrop.fold(
      Event.Move(_, situation, state, clockEvent, updated.crazyData),
      Event.Drop(_, situation, state, clockEvent, updated.crazyData)
    ) ::
      {
        // abstraction leak, I know.
        (updated.variant.threeCheck && situation.check) ?? List(Event.CheckCount(
          white = updated.checkCount.white,
          black = updated.checkCount.black
        ))
      }.toList

    Progress(this, updated, events)
  }

  def check = castleLastMoveTime.check

  def updatePlayer(color: Color, f: Player => Player) = color.fold(
    copy(whitePlayer = f(whitePlayer)),
    copy(blackPlayer = f(blackPlayer)))

  def updatePlayers(f: Player => Player) = copy(
    whitePlayer = f(whitePlayer),
    blackPlayer = f(blackPlayer))

  def start = started.fold(this, copy(
    status = Status.Started,
    mode = Mode(mode.rated && userIds.distinct.size == 2),
    updatedAt = DateTime.now.some
  ))

  def correspondenceClock: Option[CorrespondenceClock] = daysPerTurn map { days =>
    val increment = days * 24 * 60 * 60
    val secondsLeft = lastMoveDateTime.fold(increment) { lmd =>
      (lmd.getSeconds + increment - nowSeconds).toInt max 0
    }
    CorrespondenceClock(
      increment = increment,
      whiteTime = turnColor.fold(secondsLeft, increment),
      blackTime = turnColor.fold(increment, secondsLeft))
  }

  def playableCorrespondenceClock: Option[CorrespondenceClock] =
    playable ?? correspondenceClock

  def speed = chess.Speed(clock)

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
    player(color).lastDrawOffer ?? (_ >= turns - 1)

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

  def moretimeable(color: Color) =
    playable && nonMandatory && clock.??(_ moretimeable color)

  def abortable = status == Status.Started && playedTurns < 2 && nonMandatory

  def berserkable = clock.??(_.berserkable) && status == Status.Started && playedTurns < 2

  def goBerserk(color: Color) =
    clock.ifTrue(berserkable && !player(color).berserk).map { c =>
      val newClock = c berserk color
      withClock(newClock).map(_.withPlayer(color, _.goBerserk)) +
        Event.Clock(newClock) +
        Event.Berserk(color)
    }

  def withPlayer(color: Color, f: Player => Player) = copy(
    whitePlayer = if (color.white) f(whitePlayer) else whitePlayer,
    blackPlayer = if (color.black) f(blackPlayer) else blackPlayer)

  def resignable = playable && !abortable
  def drawable = playable && !abortable

  def finish(status: Status, winner: Option[Color]) = Progress(
    this,
    copy(
      status = status,
      whitePlayer = whitePlayer.finish(winner contains White),
      blackPlayer = blackPlayer.finish(winner contains Black),
      clock = clock map (_.stop)
    ),
    List(Event.End(winner)) ::: clock.??(c => List(Event.Clock(c)))
  )

  def rated = mode.rated
  def casual = !rated

  def finished = status >= Status.Mate

  def finishedOrAborted = finished || aborted

  def accountable = playedTurns >= 2 || isTournament

  def replayable = isPgnImport || finished

  def analysable =
    replayable && playedTurns > 4 &&
      Game.analysableVariants(variant) &&
      !Game.isOldHorde(this)

  def ratingVariant =
    if (isTournament && variant == chess.variant.FromPosition) chess.variant.Standard
    else variant

  def fromPosition = variant == chess.variant.FromPosition || source.??(Source.Position==)

  def imported = source contains Source.Import

  def winner = players find (_.wins)

  def loser = winner map opponent

  def winnerColor: Option[Color] = winner map (_.color)

  def winnerUserId: Option[String] = winner flatMap (_.userId)

  def loserUserId: Option[String] = loser flatMap (_.userId)

  def wonBy(c: Color): Option[Boolean] = winnerColor map (_ == c)

  def lostBy(c: Color): Option[Boolean] = winnerColor map (_ != c)

  def drawn = finished && winner.isEmpty

  def outoftime(playerLag: Color => Int): Boolean =
    outoftimeClock(playerLag) || outoftimeCorrespondence

  private def outoftimeClock(playerLag: Color => Int): Boolean = clock ?? { c =>
    started && playable && (bothPlayersHaveMoved || isSimul) && {
      (!c.isRunning && !c.isInit) || c.outoftimeWithGrace(player.color, playerLag(player.color))
    }
  }

  private def outoftimeCorrespondence: Boolean =
    playableCorrespondenceClock ?? { _ outoftime player.color }

  def isCorrespondence = speed == chess.Speed.Correspondence

  def isSwitchable = nonAi && (isCorrespondence || isSimul)

  def hasClock = clock.isDefined

  def hasCorrespondenceClock = daysPerTurn.isDefined

  def isUnlimited = !hasClock && !hasCorrespondenceClock

  def isClockRunning = clock ?? (_.isRunning)

  def withClock(c: Clock) = Progress(this, copy(clock = Some(c)))

  def estimateClockTotalTime = clock.map(_.estimateTotalTime)

  def estimateTotalTime = estimateClockTotalTime orElse
    correspondenceClock.map(_.estimateTotalTime) getOrElse 1200

  def playerWhoDidNotMove: Option[Player] = playedTurns match {
    case 0 => player(White).some
    case 1 => player(Black).some
    case _ => none
  }

  def onePlayerHasMoved = playedTurns > 0
  def bothPlayersHaveMoved = playedTurns > 1

  def startColor = Color(startedAtTurn % 2 == 0)

  def playerMoves(color: Color): Int =
    if (color == startColor) (playedTurns + 1) / 2
    else playedTurns / 2

  def playerHasMoved(color: Color) = playerMoves(color) > 0

  def playerBlurPercent(color: Color): Int = (playedTurns > 5).fold(
    (player(color).blurs * 100) / playerMoves(color),
    0
  )

  def isBeingPlayed = !isPgnImport && !finishedOrAborted

  def olderThan(seconds: Int) = (updatedAt | createdAt) isBefore DateTime.now.minusSeconds(seconds)

  def unplayed = !bothPlayersHaveMoved && (createdAt isBefore Game.unplayedDate)

  def abandoned = (status <= Status.Started) && ((updatedAt | createdAt) isBefore hasAi.fold(Game.aiAbandonedDate, Game.abandonedDate))

  def forecastable = started && playable && isCorrespondence && !hasAi

  def hasBookmarks = bookmarks > 0

  def showBookmarks = hasBookmarks ?? bookmarks.toString

  def userIds = playerMaps(_.userId)

  def userRatings = playerMaps(_.rating)

  def averageUsersRating = userRatings match {
    case a :: b :: Nil => Some((a + b) / 2)
    case a :: Nil      => Some((a + 1500) / 2)
    case _             => None
  }

  def withTournamentId(id: String) = this.copy(
    metadata = metadata.copy(tournamentId = id.some))

  def withSimulId(id: String) = this.copy(
    metadata = metadata.copy(simulId = id.some))

  def withId(newId: String) = this.copy(id = newId)

  def source = metadata.source

  def pgnImport = metadata.pgnImport
  def isPgnImport = pgnImport.isDefined

  def resetTurns = copy(turns = 0, startedAtTurn = 0)

  lazy val opening: Option[FullOpening.AtPly] =
    if (fromPosition || !Variant.openingSensibleVariants(variant)) none
    else FullOpeningDB search pgnMoves

  def synthetic = id == Game.syntheticId

  private def playerMaps[A](f: Player => Option[A]): List[A] = players flatMap { f(_) }

  def pov(c: Color) = Pov(this, c)
  def whitePov = pov(White)
  def blackPov = pov(Black)
}

object Game {

  type ID = String

  case class WithInitialFen(game: Game, fen: Option[FEN])

  val syntheticId = "synthetic"

  val maxPlayingRealtime = 125 // plus 200 correspondence games

  val analysableVariants: Set[Variant] = Set(
    chess.variant.Standard,
    chess.variant.Crazyhouse,
    chess.variant.Chess960,
    chess.variant.KingOfTheHill,
    chess.variant.ThreeCheck,
    chess.variant.FromPosition,
    chess.variant.Horde,
    chess.variant.Atomic,
    chess.variant.RacingKings)

  val unanalysableVariants: Set[Variant] = Variant.all.toSet -- analysableVariants

  val variantsWhereWhiteIsBetter: Set[Variant] = Set(
    chess.variant.ThreeCheck,
    chess.variant.Atomic,
    chess.variant.Horde,
    chess.variant.RacingKings,
    chess.variant.Antichess)

  val visualisableVariants: Set[Variant] = Set(
    chess.variant.Standard,
    chess.variant.Chess960)

  val hordeWhitePawnsSince = new DateTime(2015, 4, 11, 10, 0)

  def isOldHorde(game: Game) =
    game.variant == chess.variant.Horde &&
      game.createdAt.isBefore(Game.hordeWhitePawnsSince)

  val gameIdSize = 8
  val playerIdSize = 4
  val fullIdSize = 12
  val tokenSize = 4

  val unplayedHours = 24
  def unplayedDate = DateTime.now minusHours unplayedHours

  val abandonedDays = 21
  def abandonedDate = DateTime.now minusDays abandonedDays

  val aiAbandonedDays = 3
  def aiAbandonedDate = DateTime.now minusDays aiAbandonedDays

  def takeGameId(fullId: String) = fullId take gameIdSize
  def takePlayerId(fullId: String) = fullId drop gameIdSize

  def make(
    game: ChessGame,
    whitePlayer: Player,
    blackPlayer: Player,
    mode: Mode,
    variant: Variant,
    source: Source,
    pgnImport: Option[PgnImport],
    daysPerTurn: Option[Int] = None): Game = Game(
    id = IdGenerator.game,
    whitePlayer = whitePlayer,
    blackPlayer = blackPlayer,
    binaryPieces =
    if (game.isStandardInit) BinaryFormat.piece.standard
    else BinaryFormat.piece write game.board.pieces,
    binaryPgn = ByteArray.empty,
    status = Status.Created,
    turns = game.turns,
    startedAtTurn = game.startedAtTurn,
    clock = game.clock,
    castleLastMoveTime = CastleLastMoveTime.init.copy(castles = game.board.history.castles),
    unmovedRooks = game.board.unmovedRooks,
    daysPerTurn = daysPerTurn,
    mode = mode,
    variant = variant,
    crazyData = (variant == Crazyhouse) option Crazyhouse.Data.init,
    metadata = Metadata(
      source = source.some,
      pgnImport = pgnImport,
      tournamentId = none,
      simulId = none,
      tvAt = none,
      analysed = false),
    createdAt = DateTime.now)

  object BSONFields {

    val id = "_id"
    val whitePlayer = "p0"
    val blackPlayer = "p1"
    val playerIds = "is"
    val playerUids = "us"
    val playingUids = "pl"
    val binaryPieces = "ps"
    val binaryPgn = "pg"
    val status = "s"
    val turns = "t"
    val startedAtTurn = "st"
    val clock = "c"
    val positionHashes = "ph"
    val checkCount = "cc"
    val castleLastMoveTime = "cl"
    val unmovedRooks = "ur"
    val daysPerTurn = "cd"
    val moveTimes = "mt"
    val rated = "ra"
    val analysed = "an"
    val variant = "v"
    val crazyData = "chd"
    val next = "ne"
    val bookmarks = "bm"
    val createdAt = "ca"
    val updatedAt = "ua"
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

case class CastleLastMoveTime(
    castles: Castles,
    lastMove: Option[(Pos, Pos)],
    lastMoveTime: Option[Int], // tenths of seconds since game creation
    check: Option[Pos]) {

  def lastMoveString = lastMove map { case (a, b) => s"$a$b" }
}

object CastleLastMoveTime {

  def init = CastleLastMoveTime(Castles.all, None, None, None)

  import reactivemongo.bson._
  import lila.db.ByteArray.ByteArrayBSONHandler

  private[game] implicit val castleLastMoveTimeBSONHandler = new BSONHandler[BSONBinary, CastleLastMoveTime] {
    def read(bin: BSONBinary) = BinaryFormat.castleLastMoveTime read {
      ByteArrayBSONHandler read bin
    }
    def write(clmt: CastleLastMoveTime) = ByteArrayBSONHandler write {
      BinaryFormat.castleLastMoveTime write clmt
    }
  }
}
