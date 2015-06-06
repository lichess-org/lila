package lila.game

import chess.Color.{ White, Black }
import chess.Pos.piotr, chess.Role.forsyth
import chess.variant.Variant
import chess.{ History => ChessHistory, CheckCount, Castles, Role, Board, Move, Pos, Game => ChessGame, Clock, Status, Color, Piece, Mode, PositionHash }
import org.joda.time.DateTime

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
    daysPerTurn: Option[Int],
    positionHashes: PositionHash = Array(),
    checkCount: CheckCount = CheckCount(0, 0),
    binaryMoveTimes: ByteArray = ByteArray.empty, // tenths of seconds
    mode: Mode = Mode.default,
    variant: Variant = Variant.default,
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

  def playerByUserId(userId: String): Option[Player] = players find (_.userId == Some(userId))

  def opponent(p: Player): Player = opponent(p.color)

  def opponent(c: Color): Player = player(!c)

  lazy val firstColor = (whitePlayer before blackPlayer).fold(White, Black)
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

  private def lastMoveTimeDate: Option[DateTime] = castleLastMoveTime.lastMoveTime map { lmt =>
    createdAt plus (lmt * 100l)
  } orElse updatedAt

  def updatedAtOrCreatedAt = updatedAt | createdAt

  def lastMoveTimeInSeconds: Option[Int] = lastMoveTime.map(x => (x / 10).toInt)

  // in tenths of seconds
  lazy val moveTimes: Vector[Int] = BinaryFormat.moveTime read binaryMoveTimes take playedTurns

  def moveTimesInSeconds: Vector[Float] = moveTimes.map(_.toFloat / 10)

  /**
   * Fullmove number: The number of the full move.
   * It starts at 1, and is incremented after Black's move.
   * NOTE: Duplicates chess.Game.fullMoveNumber (avoids loading toChess)
   */
  def fullMoveNumber: Int = 1 + turns / 2

  lazy val pgnMoves: PgnMoves = BinaryFormat.pgn read binaryPgn

  def openingPgnMoves(nb: Int): PgnMoves = BinaryFormat.pgn.read(binaryPgn, nb)

  lazy val toChess: ChessGame = {

    val pieces = BinaryFormat.piece.read(binaryPieces, variant)

    ChessGame(
      board = Board(pieces, toChessHistory, variant),
      player = Color(0 == turns % 2),
      clock = clock,
      turns = turns,
      startedAtTurn = startedAtTurn,
      pgnMoves = pgnMoves)
  }

  lazy val toChessHistory = ChessHistory(
    lastMove = castleLastMoveTime.lastMove,
    castles = castleLastMoveTime.castles,
    positionHashes = positionHashes,
    checkCount = checkCount)

  def update(
    game: ChessGame,
    move: Move,
    blur: Boolean = false): Progress = {
    val (history, situation) = (game.board.history, game.situation)

    def copyPlayer(player: Player) = player.copy(
      blurs = math.min(
        playerMoves(player.color),
        player.blurs + (blur && move.color == player.color).fold(1, 0))
    )

    val updated = copy(
      whitePlayer = copyPlayer(whitePlayer),
      blackPlayer = copyPlayer(blackPlayer),
      binaryPieces = BinaryFormat.piece write game.board.pieces,
      binaryPgn = BinaryFormat.pgn write game.pgnMoves,
      turns = game.turns,
      positionHashes = history.positionHashes,
      checkCount = history.checkCount,
      castleLastMoveTime = CastleLastMoveTime(
        castles = history.castles,
        lastMove = history.lastMove,
        lastMoveTime = Some(((nowMillis - createdAt.getMillis) / 100).toInt),
        check = situation.kingPos ifTrue situation.check),
      binaryMoveTimes = isPgnImport.fold(
        ByteArray.empty,
        BinaryFormat.moveTime write lastMoveTime.fold(Vector(0)) { lmt => moveTimes :+ (nowTenths - lmt).toInt }
      ),
      status = situation.status | status,
      clock = game.clock)

    val events = (players collect {
      case p if p.isHuman => Event.possibleMoves(situation, p.color)
    }) :::
      Event.State(
        situation.color,
        game.turns,
        (status != updated.status) option status,
        whiteOffersDraw = whitePlayer.isOfferingDraw,
        blackOffersDraw = blackPlayer.isOfferingDraw) ::
        Event.fromMove(move, situation) :::
        (Event fromSituation situation)

    val clockEvent = updated.clock map Event.Clock.apply orElse {
      updated.correspondenceClock map Event.CorrespondenceClock.apply
    }

    val finalEvents = events ::: clockEvent.toList ::: {
      // abstraction leak, I know.
      (updated.variant.threeCheck && situation.check) ?? List(Event.CheckCount(
        white = updated.checkCount.white,
        black = updated.checkCount.black
      ))
    }

    Progress(this, updated, finalEvents)
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

  def correspondenceClock: Option[CorrespondenceClock] =
    daysPerTurn ifTrue playable map { days =>
      val increment = days * 24 * 60 * 60
      val secondsLeft = lastMoveTimeDate.fold(increment) { lmd =>
        (lmd.getSeconds + increment - nowSeconds).toInt max 0
      }
      CorrespondenceClock(
        increment = increment,
        whiteTime = turnColor.fold(secondsLeft, increment),
        blackTime = turnColor.fold(increment, secondsLeft))
    }

  def speed = chess.Speed(clock)

  def perfKey = PerfPicker.key(this)
  def perfType = PerfType(perfKey)

  def started = status >= Status.Started

  def notStarted = !started

  def joinable = notStarted && !imported

  def aborted = status == Status.Aborted

  def playable = status < Status.Aborted

  def playableBy(p: Player): Boolean = playable && turnOf(p)

  def playableBy(c: Color): Boolean = playableBy(player(c))

  def playableByAi: Boolean = playable && player.isAi

  def continuable = status != Status.Mate && status != Status.Stalemate

  def aiLevel: Option[Int] = players find (_.isAi) flatMap (_.aiLevel)

  def hasAi: Boolean = players exists (_.isAi)
  def nonAi = !hasAi

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
      nonMandatory

  def playerCanProposeTakeback(color: Color) =
    started && playable && !isTournament && !isSimul &&
      bothPlayersHaveMoved &&
      !player(color).isProposingTakeback &&
      !opponent(color).isProposingTakeback

  def moretimeable(color: Color) =
    playable && nonMandatory && clock.??(_ moretimeable color)

  def abortable = status == Status.Started && playedTurns < 2 && nonMandatory

  def berserkable = status == Status.Started && playedTurns < 2

  def resignable = playable && !abortable
  def drawable = playable && !abortable

  def finish(status: Status, winner: Option[Color]) = Progress(
    this,
    copy(
      status = status,
      whitePlayer = whitePlayer finish (winner == Some(White)),
      blackPlayer = blackPlayer finish (winner == Some(Black)),
      clock = clock map (_.stop)
    ),
    List(Event.End) ::: clock.??(c => List(Event.Clock(c)))
  )

  def rated = mode.rated
  def casual = !rated

  def finished = status >= Status.Mate

  def finishedOrAborted = finished || aborted

  def accountable = playedTurns >= 2 || isTournament

  def replayable = imported || finished

  def analysable = replayable && playedTurns > 4 && Game.analysableVariants(variant)

  def fromPosition = source ?? (Source.Position==)

  def imported = source exists (_ == Source.Import)

  def winner = players find (_.wins)

  def loser = winner map opponent

  def winnerColor: Option[Color] = winner map (_.color)

  def winnerUserId: Option[String] = winner flatMap (_.userId)

  def loserUserId: Option[String] = loser flatMap (_.userId)

  def wonBy(c: Color): Option[Boolean] = winnerColor map (_ == c)

  def outoftimePlayer: Option[Player] =
    outoftimePlayerClock orElse outoftimePlayerCorrespondence

  private def outoftimePlayerClock: Option[Player] = for {
    c ← clock
    if started && playable && (bothPlayersHaveMoved || isSimul)
    if (!c.isRunning && !c.isInit) || (c outoftime player.color)
  } yield player

  private def outoftimePlayerCorrespondence: Option[Player] = for {
    c ← correspondenceClock
    if c outoftime player.color
  } yield player

  def isCorrespondence = speed == chess.Speed.Correspondence

  def hasClock = clock.isDefined

  def hasCorrespondenceClock = daysPerTurn.isDefined

  def isUnlimited = !hasClock && !hasCorrespondenceClock

  def isClockRunning = clock ?? (_.isRunning)

  def withClock(c: Clock) = Progress(this, copy(clock = Some(c)))

  def estimateTotalTime =
    clock.map(_.estimateTotalTime) orElse
      correspondenceClock.map(_.estimateTotalTime) getOrElse 1200

  def playerWhoDidNotMove: Option[Player] = playedTurns match {
    case 0 => player(White).some
    case 1 => player(Black).some
    case _ => none
  }

  def onePlayerHasMoved = playedTurns > 0
  def bothPlayersHaveMoved = playedTurns > 1

  def playerMoves(color: Color): Int = (playedTurns + color.fold(1, 0)) / 2

  def playerHasMoved(color: Color) = playerMoves(color) > 0

  def playerBlurPercent(color: Color): Int = (playedTurns > 5).fold(
    (player(color).blurs * 100) / playerMoves(color),
    0
  )

  def isBeingPlayed = !isPgnImport && !finishedOrAborted

  def olderThan(seconds: Int) = (updatedAt | createdAt) isBefore DateTime.now.minusSeconds(seconds)

  def unplayed = !bothPlayersHaveMoved && (createdAt isBefore Game.unplayedDate)

  def abandoned = (status <= Status.Started) && ((updatedAt | createdAt) isBefore hasAi.fold(Game.aiAbandonedDate, Game.abandonedDate))

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

  lazy val opening: Option[chess.OpeningExplorer.Opening] =
    if (playable || fromPosition || !Game.openingSensiblevariants(variant)) none
    else chess.OpeningExplorer openingOf pgnMoves

  private def playerMaps[A](f: Player => Option[A]): List[A] = players flatMap { f(_) }
}

object Game {

  val openingSensiblevariants: Set[Variant] = Set(
    chess.variant.Standard,
    chess.variant.ThreeCheck,
    chess.variant.KingOfTheHill)

  val analysableVariants: Set[Variant] = Set(
    chess.variant.Standard,
    chess.variant.Chess960,
    chess.variant.KingOfTheHill,
    chess.variant.ThreeCheck)

  val unanalysableVariants: Set[Variant] = Variant.all.toSet -- analysableVariants

  val variantsWhereWhiteIsBetter: Set[Variant] = Set(
    chess.variant.ThreeCheck,
    chess.variant.Atomic,
    chess.variant.Horde,
    chess.variant.Antichess)

  val gameIdSize = 8
  val playerIdSize = 4
  val fullIdSize = 12
  val tokenSize = 4

  val unplayedHours = 24
  def unplayedDate = DateTime.now minusHours unplayedHours

  val abandonedDays = 15
  def abandonedDate = DateTime.now minusDays abandonedDays

  val aiAbandonedDays = 3
  def aiAbandonedDate = DateTime.now minusDays abandonedDays

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
    castles: Castles = Castles.init,
    daysPerTurn: Option[Int] = None): Game = Game(
    id = IdGenerator.game,
    whitePlayer = whitePlayer,
    blackPlayer = blackPlayer,
    binaryPieces = if (game.isStandardInit) BinaryFormat.piece.standard
    else BinaryFormat.piece write game.board.pieces,
    binaryPgn = ByteArray.empty,
    status = Status.Created,
    turns = game.turns,
    startedAtTurn = game.startedAtTurn,
    clock = game.clock,
    castleLastMoveTime = CastleLastMoveTime.init.copy(castles = castles),
    daysPerTurn = daysPerTurn,
    mode = mode,
    variant = variant,
    metadata = Metadata(
      source = source.some,
      pgnImport = pgnImport,
      tournamentId = none,
      simulId = none,
      tvAt = none,
      analysed = false),
    createdAt = DateTime.now)

  private[game] lazy val tube = lila.db.BsTube(gameBSONHandler)

  import reactivemongo.bson._
  import lila.db.BSON
  import Player.playerBSONHandler
  import PgnImport.pgnImportBSONHandler
  import CastleLastMoveTime.castleLastMoveTimeBSONHandler

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
    val daysPerTurn = "cd"
    val moveTimes = "mt"
    val rated = "ra"
    val analysed = "an"
    val variant = "v"
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

  private[game] implicit val checkCountWriter = new BSONWriter[CheckCount, BSONArray] {
    def write(cc: CheckCount) = BSONArray(cc.white, cc.black)
  }

  implicit val gameBSONHandler = new BSON[Game] {

    import BSONFields._

    private val emptyPlayerBuilder = playerBSONHandler.read(BSONDocument())

    def reads(r: BSON.Reader): Game = {
      val nbTurns = r int turns
      val winC = r boolO winnerColor map Color.apply
      val (whiteId, blackId) = r str playerIds splitAt 4
      val uids = ~r.getO[List[String]](playerUids)
      val (whiteUid, blackUid) = (uids.headOption.filter(_.nonEmpty), uids.lift(1).filter(_.nonEmpty))
      def player(field: String, color: Color, id: Player.Id, uid: Player.UserId): Player = {
        val builder = r.getO[Player.Builder](field)(playerBSONHandler) | emptyPlayerBuilder
        val win = winC map (_ == color)
        builder(color)(id)(uid)(win)
      }
      val createdAtValue = r date createdAt
      Game(
        id = r str id,
        whitePlayer = player(whitePlayer, White, whiteId, whiteUid),
        blackPlayer = player(blackPlayer, Black, blackId, blackUid),
        binaryPieces = r bytes binaryPieces,
        binaryPgn = r bytesD binaryPgn,
        status = Status(r int status) err "game invalid status",
        turns = nbTurns,
        startedAtTurn = r intD startedAtTurn,
        clock = r.getO[Color => Clock](clock)(clockBSONHandler(createdAtValue)) map (_(Color(0 == nbTurns % 2))),
        positionHashes = r.bytesD(positionHashes).value,
        checkCount = {
          val counts = r.intsD(checkCount)
          CheckCount(~counts.headOption, ~counts.lastOption)
        },
        castleLastMoveTime = r.get[CastleLastMoveTime](castleLastMoveTime)(castleLastMoveTimeBSONHandler),
        daysPerTurn = r intO daysPerTurn,
        binaryMoveTimes = (r bytesO moveTimes) | ByteArray.empty,
        mode = Mode(r boolD rated),
        variant = Variant(r intD variant) | chess.variant.Standard,
        next = r strO next,
        bookmarks = r intD bookmarks,
        createdAt = createdAtValue,
        updatedAt = r dateO updatedAt,
        metadata = Metadata(
          source = r intO source flatMap Source.apply,
          pgnImport = r.getO[PgnImport](pgnImport)(PgnImport.pgnImportBSONHandler),
          tournamentId = r strO tournamentId,
          simulId = r strO simulId,
          tvAt = r dateO tvAt,
          analysed = r boolD analysed)
      )
    }

    def writes(w: BSON.Writer, o: Game) = BSONDocument(
      id -> o.id,
      playerIds -> (o.whitePlayer.id + o.blackPlayer.id),
      playerUids -> w.listO(List(~o.whitePlayer.userId, ~o.blackPlayer.userId)),
      whitePlayer -> w.docO(playerBSONHandler write ((_: Color) => (_: Player.Id) => (_: Player.UserId) => (_: Player.Win) => o.whitePlayer)),
      blackPlayer -> w.docO(playerBSONHandler write ((_: Color) => (_: Player.Id) => (_: Player.UserId) => (_: Player.Win) => o.blackPlayer)),
      binaryPieces -> o.binaryPieces,
      binaryPgn -> w.byteArrayO(o.binaryPgn),
      status -> o.status.id,
      turns -> o.turns,
      startedAtTurn -> w.intO(o.startedAtTurn),
      clock -> (o.clock map { c => clockBSONHandler(o.createdAt).write(_ => c) }),
      positionHashes -> w.bytesO(o.positionHashes),
      checkCount -> o.checkCount.nonEmpty.option(o.checkCount),
      castleLastMoveTime -> castleLastMoveTimeBSONHandler.write(o.castleLastMoveTime),
      daysPerTurn -> o.daysPerTurn,
      moveTimes -> (BinaryFormat.moveTime write o.moveTimes),
      rated -> w.boolO(o.mode.rated),
      variant -> o.variant.exotic.option(o.variant.id).map(w.int),
      next -> o.next,
      bookmarks -> w.intO(o.bookmarks),
      createdAt -> w.date(o.createdAt),
      updatedAt -> o.updatedAt.map(w.date),
      source -> o.metadata.source.map(_.id),
      pgnImport -> o.metadata.pgnImport,
      tournamentId -> o.metadata.tournamentId,
      simulId -> o.metadata.simulId,
      tvAt -> o.metadata.tvAt.map(w.date),
      analysed -> w.boolO(o.metadata.analysed)
    )
  }

  import lila.db.ByteArray.ByteArrayBSONHandler

  def clockBSONHandler(since: DateTime) = new BSONHandler[BSONBinary, Color => Clock] {
    def read(bin: BSONBinary) = BinaryFormat clock since read {
      ByteArrayBSONHandler read bin
    }
    def write(clock: Color => Clock) = ByteArrayBSONHandler write {
      BinaryFormat clock since write clock(chess.White)
    }
  }
}

case class CastleLastMoveTime(
    castles: Castles,
    lastMove: Option[(Pos, Pos)],
    lastMoveTime: Option[Int], // tenths of seconds since game creation
    check: Option[Pos]) {

  def lastMoveString = lastMove map { case (a, b) => a.toString + b.toString }
}

object CastleLastMoveTime {

  def init = CastleLastMoveTime(Castles.all, None, None, None)

  import reactivemongo.bson._
  import lila.db.ByteArray.ByteArrayBSONHandler

  implicit val castleLastMoveTimeBSONHandler = new BSONHandler[BSONBinary, CastleLastMoveTime] {
    def read(bin: BSONBinary) = BinaryFormat.castleLastMoveTime read {
      ByteArrayBSONHandler read bin
    }
    def write(clmt: CastleLastMoveTime) = ByteArrayBSONHandler write {
      BinaryFormat.castleLastMoveTime write clmt
    }
  }
}
