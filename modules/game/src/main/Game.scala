package lila.game

import chess.Color._
import chess.Pos.piotr, chess.Role.forsyth
import chess.{ History => ChessHistory, Castles, Role, Board, Move, Pos, Game => ChessGame, Clock, Status, Color, Piece, Variant, Mode, PositionHash }
import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime

import lila.db.ByteArray
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
    positionHashes: PositionHash = Array(),
    moveTimes: Vector[Int] = Vector.empty, // tenths of seconds
    mode: Mode = Mode.default,
    variant: Variant = Variant.default,
    next: Option[String] = None,
    bookmarks: Int = 0,
    createdAt: DateTime = DateTime.now,
    updatedAt: Option[DateTime] = None,
    metadata: Metadata) {

  val players = List(whitePlayer, blackPlayer)

  val playersByColor: Map[Color, Player] = Map(
    White -> whitePlayer,
    Black -> blackPlayer
  )

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

  def opponent(p: Player): Player = opponent(p.color)

  def opponent(c: Color): Player = player(!c)

  private lazy val firstColor = (whitePlayer before blackPlayer).fold(White, Black)
  def firstPlayer = player(firstColor)
  def secondPlayer = player(!firstColor)

  def turnColor = Color(0 == turns % 2)

  def turnOf(p: Player): Boolean = p == player
  def turnOf(c: Color): Boolean = c == turnColor
  def turnOf(u: User): Boolean = player(u) ?? turnOf

  def playedTurns = turns - startedAtTurn

  def fullIdOf(player: Player): Option[String] =
    (players contains player) option id + player.id

  def fullIdOf(color: Color): String = id + player(color).id

  def tournamentId = metadata.tournamentId

  def isTournament = tournamentId.isDefined
  def nonTournament = tournamentId.isEmpty

  def hasChat = nonTournament && nonAi

  // in tenths
  private def lastMoveTime: Option[Long] = castleLastMoveTime.lastMoveTime map {
    _.toLong + (createdAt.getMillis / 100)
  }
  def lastMoveTimeInSeconds: Option[Int] = lastMoveTime.map(x => (x / 10).toInt)

  def moveTimesInSeconds: Vector[Float] = moveTimes.map(_.toFloat / 10)

  lazy val pgnMoves: PgnMoves = BinaryFormat.pgn read binaryPgn

  lazy val toChess: ChessGame = {

    val (pieces, deads) = BinaryFormat.piece read binaryPieces

    ChessGame(
      board = Board(pieces, toChessHistory, variant),
      player = Color(0 == turns % 2),
      clock = clock,
      deads = deads,
      turns = turns,
      startedAtTurn = startedAtTurn,
      pgnMoves = pgnMoves)
  }

  lazy val toChessHistory = ChessHistory(
    lastMove = castleLastMoveTime.lastMove,
    castles = castleLastMoveTime.castles,
    positionHashes = positionHashes)

  def update(
    game: ChessGame,
    move: Move,
    blur: Boolean = false): Progress = {
    val (history, situation) = (game.board.history, game.situation)

    val events = (players collect {
      case p if p.isHuman => Event.possibleMoves(situation, p.color)
    }) :::
      Event.State(situation.color, game.turns) ::
      (Event fromMove move) :::
      (Event fromSituation situation)

    def copyPlayer(player: Player) = player.copy(
      blurs = player.blurs + (blur && move.color == player.color).fold(1, 0)
    )

    val updated = copy(
      whitePlayer = copyPlayer(whitePlayer),
      blackPlayer = copyPlayer(blackPlayer),
      binaryPieces = BinaryFormat.piece write game.allPieces,
      binaryPgn = BinaryFormat.pgn write game.pgnMoves,
      turns = game.turns,
      positionHashes = history.positionHashes,
      castleLastMoveTime = CastleLastMoveTime(
        castles = history.castles,
        lastMove = history.lastMove,
        lastMoveTime = Some(((nowMillis - createdAt.getMillis) / 100).toInt),
        check = situation.kingPos ifTrue situation.check),
      moveTimes = isPgnImport.fold(
        Vector.empty,
        lastMoveTime.fold(Vector(0)) { lmt => moveTimes :+ (nowTenths - lmt).toInt }
      ),
      status = situation.status | status,
      clock = game.clock)

    val finalEvents = events ::: updated.clock.??(c => List(Event.Clock(c))) ::: {
      (updated.playable && (
        abortable != updated.abortable || (Color.all exists { color =>
          playerCanOfferDraw(color) != updated.playerCanOfferDraw(color)
        })
      )).??(Color.all map Event.ReloadTable)
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

  def startClock(compensation: Float) = copy(
    clock = clock map {
      case c: chess.PausedClock => c.start.giveTime(White, compensation)
      case c                    => c
    }
  )

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
      nonTournament

  def playerCanProposeTakeback(color: Color) =
    started && playable && nonTournament &&
      bothPlayersHaveMoved &&
      !player(color).isProposingTakeback &&
      !opponent(color).isProposingTakeback

  def moretimeable = playable && nonTournament && hasClock

  def abortable = status == Status.Started && turns < 2 && nonTournament

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

  def replayable = imported || finished

  def analysable = replayable && !fromPosition && turns > 4

  def fromPosition = source ?? (Source.Position==)

  def imported = source exists (_ == Source.Import)

  def winner = players find (_.wins)

  def loser = winner map opponent

  def winnerColor: Option[Color] = winner map (_.color)

  def winnerUserId: Option[String] = winner flatMap (_.userId)

  def loserUserId: Option[String] = loser flatMap (_.userId)

  def wonBy(c: Color): Option[Boolean] = winnerColor map (_ == c)

  def outoftimePlayer: Option[Player] = for {
    c ← clock
    if started && playable && (bothPlayersHaveMoved || isTournament)
    if (!c.isRunning && !c.isInit) || (c outoftime player.color)
  } yield player

  def hasClock = clock.isDefined

  def isClockRunning = clock ?? (_.isRunning)

  def withClock(c: Clock) = Progress(this, copy(clock = Some(c)))

  def estimateTotalTime = clock.fold(1200)(_.estimateTotalTime)

  def playerWhoDidNotMove: Option[Player] = turns match {
    case 0 => player(White).some
    case 1 => player(Black).some
    case _ => none
  }

  def onePlayerHasMoved = turns > 0
  def bothPlayersHaveMoved = turns > 1

  def playerMoves(color: Color): Int = (turns + color.fold(1, 0)) / 2

  def playerHasMoved(color: Color) = playerMoves(color) > 0

  def playerBlurPercent(color: Color): Int = (turns > 5).fold(
    (player(color).blurs * 100) / playerMoves(color),
    0
  )

  def deadPiecesOf(color: Color): List[Role] = toChess.deads collect {
    case piece if piece is color => piece.role
  }

  def isBeingPlayed = !finishedOrAborted && !olderThan(60)

  def olderThan(seconds: Int) = updatedAt.??(_ < DateTime.now - seconds.seconds)

  def abandoned = (status <= Status.Started) && (updatedAt | createdAt) < Game.abandonedDate

  def hasBookmarks = bookmarks > 0

  def showBookmarks = hasBookmarks ?? bookmarks.toString

  def userIds = playerMaps(_.userId)

  def userRatings = playerMaps(_.rating)

  def averageUsersRating = userRatings match {
    case a :: b :: Nil => Some((a + b) / 2)
    case a :: Nil      => Some((a + 1200) / 2)
    case _             => None
  }

  def withTournamentId(id: String) = this.copy(
    metadata = metadata.copy(tournamentId = id.some)
  )

  def withId(newId: String) = this.copy(id = newId)

  def source = metadata.source

  def pgnImport = metadata.pgnImport

  def isPgnImport = pgnImport.isDefined

  def resetTurns = copy(turns = 0)

  private def playerMaps[A](f: Player => Option[A]): List[A] = players.map(f).flatten
}

object Game {

  val gameIdSize = 8
  val playerIdSize = 4
  val fullIdSize = 12
  val tokenSize = 4

  def abandonedDate = DateTime.now - 7.days

  def takeGameId(fullId: String) = fullId take gameIdSize
  def takePlayerId(fullId: String) = fullId drop gameIdSize

  def make(
    game: ChessGame,
    whitePlayer: Player,
    blackPlayer: Player,
    mode: Mode,
    variant: Variant,
    source: Source,
    pgnImport: Option[PgnImport]): Game = Game(
    id = IdGenerator.game,
    whitePlayer = whitePlayer,
    blackPlayer = blackPlayer,
    binaryPieces = if (game.isStandardInit) BinaryFormat.piece.standard
    else BinaryFormat.piece write game.allPieces,
    binaryPgn = ByteArray.empty,
    status = Status.Created,
    turns = game.turns,
    startedAtTurn = game.startedAtTurn,
    clock = game.clock,
    castleLastMoveTime = CastleLastMoveTime.init,
    mode = mode,
    variant = variant,
    metadata = Metadata(
      source = source.some,
      pgnImport = pgnImport,
      tournamentId = none,
      tvAt = none),
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
    val binaryPieces = "ps"
    val binaryPgn = "pg"
    val status = "s"
    val turns = "t"
    val startedAtTurn = "st"
    val clock = "c"
    val positionHashes = "ph"
    val castleLastMoveTime = "cl"
    val moveTimes = "mt"
    val rated = "ra"
    val variant = "v"
    val next = "ne"
    val bookmarks = "bm"
    val createdAt = "ca"
    val updatedAt = "ua"
    val source = "so"
    val pgnImport = "pgni"
    val tournamentId = "tid"
    val tvAt = "tv"
    val winnerColor = "w"
    val winnerId = "wid"
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
      Game(
        id = r str id,
        whitePlayer = player(whitePlayer, White, whiteId, whiteUid),
        blackPlayer = player(blackPlayer, Black, blackId, blackUid),
        binaryPieces = r bytes binaryPieces,
        binaryPgn = r bytesD binaryPgn,
        status = Status(r int status) err "Invalid status",
        turns = nbTurns,
        startedAtTurn = r intD startedAtTurn,
        clock = r.getO[Color => Clock](clock) map (_(Color(0 == nbTurns % 2))),
        positionHashes = r.bytesD(positionHashes).value,
        castleLastMoveTime = r.get[CastleLastMoveTime](castleLastMoveTime)(castleLastMoveTimeBSONHandler),
        moveTimes = ((r bytesO moveTimes) ?? BinaryFormat.moveTime.read _) take nbTurns,
        mode = Mode(r boolD rated),
        variant = Variant(r intD variant) | Variant.Standard,
        next = r strO next,
        bookmarks = r intD bookmarks,
        createdAt = r date createdAt,
        updatedAt = r dateO updatedAt,
        metadata = Metadata(
          source = r intO source flatMap Source.apply,
          pgnImport = r.getO[PgnImport](pgnImport)(PgnImport.pgnImportBSONHandler),
          tournamentId = r strO tournamentId,
          tvAt = r dateO tvAt)
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
      clock -> (o.clock map { c => clockBSONHandler.write(_ => c) }),
      positionHashes -> w.bytesO(o.positionHashes),
      castleLastMoveTime -> castleLastMoveTimeBSONHandler.write(o.castleLastMoveTime),
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
      tvAt -> o.metadata.tvAt.map(w.date)
    )
  }

  import lila.db.ByteArray.ByteArrayBSONHandler

  implicit val clockBSONHandler = new BSONHandler[BSONBinary, Color => Clock] {
    def read(bin: BSONBinary) = BinaryFormat.clock read {
      ByteArrayBSONHandler read bin
    }
    def write(clock: Color => Clock) = ByteArrayBSONHandler write {
      BinaryFormat.clock write clock(chess.White)
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
