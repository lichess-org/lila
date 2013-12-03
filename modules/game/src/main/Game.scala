package lila.game

import chess.Color._
import chess.Pos.piotr, chess.Role.forsyth
import chess.{ History ⇒ ChessHistory, Castles, Role, Board, Move, Pos, Game ⇒ ChessGame, Clock, Status, Color, Piece, Variant, Mode }
import org.joda.time.DateTime
import org.scala_tools.time.Imports._

import lila.db.ByteArray
import lila.user.User

case class Game(
    id: String,
    token: String,
    whitePlayer: Player,
    blackPlayer: Player,
    binaryPieces: ByteArray,
    status: Status,
    turns: Int,
    clock: Option[Clock],
    check: Option[Pos] = None,
    creatorColor: Color,
    positionHashes: String = "",
    castleLastMoveTime: CastleLastMoveTime,
    mode: Mode = Mode.default,
    variant: Variant = Variant.default,
    next: Option[String] = None,
    bookmarks: Int = 0,
    createdAt: DateTime = DateTime.now,
    updatedAt: Option[DateTime] = None,
    metadata: Option[Metadata] = None) {

  val players = List(whitePlayer, blackPlayer)

  val playersByColor: Map[Color, Player] = Map(
    White -> whitePlayer,
    Black -> blackPlayer
  )

  def player(color: Color): Player = color match {
    case White ⇒ whitePlayer
    case Black ⇒ blackPlayer
  }

  def player(playerId: String): Option[Player] =
    players find (_.id == playerId)

  def player(user: User): Option[Player] =
    players find (_ isUser user)

  def player(c: Color.type ⇒ Color): Player = player(c(Color))

  def isPlayerFullId(player: Player, fullId: String): Boolean =
    (fullId.size == Game.fullIdSize) && player.id == (fullId drop 8)

  def player: Player = player(turnColor)

  def opponent(p: Player): Player = opponent(p.color)

  def opponent(c: Color): Player = player(!c)

  def turnColor = Color(0 == turns % 2)

  def turnOf(p: Player) = p == player
  def turnOf(c: Color) = c == turnColor

  def fullIdOf(player: Player): Option[String] =
    (players contains player) option id + player.id

  def fullIdOf(color: Color): String = id + player(color).id

  def tournamentId = metadata flatMap (_.tournamentId)

  def isTournament = tournamentId.isDefined
  def nonTournament = tournamentId.isEmpty

  def hasChat = nonTournament && nonAi

  lazy val toChess: ChessGame = {

    val (pieces, deads) = BinaryFormat.piece read binaryPieces

    ChessGame(
      board = Board(pieces, toChessHistory, variant),
      player = Color(0 == turns % 2),
      clock = clock,
      deads = deads,
      turns = turns)
  }

  lazy val toChessHistory = ChessHistory(
    lastMove = castleLastMoveTime.lastMove,
    castles = castleLastMoveTime.castles,
    positionHashes = positionHashes)

  def update(
    game: ChessGame,
    move: Move,
    blur: Boolean = false): (Progress, List[String]) = {
    val (history, situation) = (game.board.history, game.situation)

    val events = (players collect {
      case p if p.isHuman ⇒ Event.possibleMoves(situation, p.color)
    }) :::
      Event.State(situation.color, game.turns) ::
      (Event fromMove move) :::
      (Event fromSituation situation)

    def copyPlayer(player: Player) = player.copy(
      blurs = player.blurs + (blur && move.color == player.color).fold(1, 0),
      moveTimes = ((!isPgnImport) && (move.color == player.color)).fold(
        castleLastMoveTime.lastMoveTime ?? { lmt ⇒
          val mt = nowSeconds - lmt
          val encoded = MoveTime encode mt
          player.moveTimes.isEmpty.fold(encoded.toString, player.moveTimes + encoded)
        }, player.moveTimes
      )
    )

    val updated = copy(
      whitePlayer = copyPlayer(whitePlayer),
      blackPlayer = copyPlayer(blackPlayer),
      binaryPieces = BinaryFormat.piece write game.allPieces,
      turns = game.turns,
      positionHashes = history.positionHashes.mkString,
      castleLastMoveTime = CastleLastMoveTime(
        castles = history.castles,
        lastMove = history.lastMove,
        lastMoveTime = nowSeconds.some),
      status = situation.status | status,
      clock = game.clock,
      check = situation.kingPos ifTrue situation.check)

    val finalEvents = events :::
      updated.clock.??(c ⇒ List(Event.Clock(c))) ::: {
        (updated.playable && (
          abortable != updated.abortable || (Color.all exists { color ⇒
            playerCanOfferDraw(color) != updated.playerCanOfferDraw(color)
          })
        )).??(Color.all map Event.ReloadTable)
      }

    Progress(this, updated, finalEvents) -> game.pgnMoves
  }

  def updatePlayer(color: Color, f: Player ⇒ Player) = color.fold(
    copy(whitePlayer = f(whitePlayer)),
    copy(blackPlayer = f(blackPlayer)))

  def updatePlayers(f: Player ⇒ Player) = copy(
    whitePlayer = f(whitePlayer),
    blackPlayer = f(blackPlayer))

  def start = started.fold(this, copy(
    status = Status.Started,
    mode = Mode(mode.rated && (players forall (_.hasUser))),
    updatedAt = DateTime.now.some
  ))

  def startClock(compensation: Float) = clock.filterNot(_.isRunning).fold(this) { c ⇒
    copy(clock = c.run.giveTime(creatorColor, compensation).some)
  }

  def hasMoveTimes = players forall (_.hasMoveTimes)

  def started = status >= Status.Started

  def notStarted = !started

  def aborted = status == Status.Aborted

  def playable = status < Status.Aborted

  def playableBy(p: Player): Boolean = playable && turnOf(p)

  def playableBy(c: Color): Boolean = playableBy(player(c))

  def playableByAi: Boolean = playable && player.isAi

  def continuable = status != Status.Mate && status != Status.Stalemate

  def aiLevel: Option[Int] = players find (_.isAi) flatMap (_.aiLevel)

  def hasAi: Boolean = players exists (_.isAi)
  def nonAi = !hasAi

  def mapPlayers(f: Player ⇒ Player) = copy(
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
    List(Event.End) ::: clock.??(c ⇒ List(Event.Clock(c)))
  )

  def rated = mode.rated

  def finished = status >= Status.Mate

  def finishedOrAborted = finished || aborted

  def analysable = (imported || finished) && (source.fold(true)(Source.Position!=))

  def imported = source exists (_ == Source.Import)

  def winner = players find (_.wins)

  def loser = winner map opponent

  def winnerColor: Option[Color] = winner map (_.color)

  def winnerUserId: Option[String] = winner flatMap (_.userId)

  def loserUserId: Option[String] = loser flatMap (_.userId)

  def wonBy(c: Color): Option[Boolean] = winnerColor map (_ == c)

  def outoftimePlayer: Option[Player] = for {
    c ← clock
    if started && playable && onePlayerHasMoved
    if !c.isRunning || (c outoftime player.color)
  } yield player

  def hasClock = clock.isDefined

  def isClockRunning = clock ?? (_.isRunning)

  def withClock(c: Clock) = Progress(this, copy(clock = Some(c)))

  def estimateTotalTime = clock.fold(1200)(_.estimateTotalTime)

  def creator = player(creatorColor)

  def invitedColor = !creatorColor

  def invited = player(invitedColor)

  def playerWhoDidNotMove: Option[Player] = turns match {
    case 0 ⇒ player(White).some
    case 1 ⇒ player(Black).some
    case _ ⇒ none
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
    case piece if piece is color ⇒ piece.role
  }

  def isBeingPlayed = !finishedOrAborted && !olderThan(60)

  def olderThan(seconds: Int) = updatedAt.??(_ < DateTime.now - seconds.seconds)

  def abandoned = (status <= Status.Started) && (updatedAt | createdAt) < Game.abandonedDate

  def hasBookmarks = bookmarks > 0

  def showBookmarks = hasBookmarks ?? bookmarks

  def userIds = playerMaps(_.userId)

  def userElos = playerMaps(_.elo)

  def averageUsersElo = userElos match {
    case a :: b :: Nil ⇒ Some((a + b) / 2)
    case a :: Nil      ⇒ Some((a + 1200) / 2)
    case _             ⇒ None
  }

  def withTournamentId(id: String) = this.copy(
    metadata = metadata map (_.copy(tournamentId = id.some)))

  def withId(newId: String) = this.copy(id = newId)

  def source = metadata flatMap (_.source)

  def pgnImport = metadata flatMap (_.pgnImport)

  def isPgnImport = pgnImport.isDefined

  def resetTurns = copy(turns = 0)

  private def playerMaps[A](f: Player ⇒ Option[A]): List[A] = players.map(f).flatten
}

object Game {

  val gameIdSize = 8
  val playerIdSize = 4
  val fullIdSize = 12
  val tokenSize = 4
  val defaultToken = "-tk-"

  def abandonedDate = DateTime.now - 7.days

  def takeGameId(fullId: String) = fullId take gameIdSize
  def takePlayerId(fullId: String) = fullId drop gameIdSize

  def make(
    game: ChessGame,
    whitePlayer: Player,
    blackPlayer: Player,
    creatorColor: Color,
    mode: Mode,
    variant: Variant,
    source: Source,
    pgnImport: Option[PgnImport]): Game = Game(
    id = IdGenerator.game,
    token = IdGenerator.token,
    whitePlayer = whitePlayer,
    blackPlayer = blackPlayer,
    binaryPieces = if (game.isStandardInit) BinaryFormat.piece.standard
    else BinaryFormat.piece write game.allPieces,
    status = Status.Created,
    turns = game.turns,
    clock = game.clock,
    check = None,
    creatorColor = creatorColor,
    positionHashes = "",
    castleLastMoveTime = CastleLastMoveTime.init,
    mode = mode,
    variant = variant,
    metadata = Metadata(
      source = source.some,
      pgnImport = pgnImport,
      tournamentId = none,
      tvAt = none).some,
    createdAt = DateTime.now)

  private[game] lazy val tube = lila.db.BsTube(gameBSONHandler)

  import reactivemongo.bson._
  import lila.db.BSON
  import Player.playerBSONHandler
  import Metadata.metadataBSONHandler
  import CastleLastMoveTime.castleLastMoveTimeBSONHandler

  object BSONFields {

    val id = "_id"
    val token = "tk"
    val players = "p"
    val binaryPieces = "ps"
    val status = "s"
    val turns = "t"
    val clock = "c"
    val check = "ck"
    val creatorColor = "cc"
    val positionHashes = "ph"
    val castleLastMoveTime = "cl"
    val rated = "ra"
    val variant = "v"
    val next = "next"
    val bookmarks = "bm"
    val createdAt = "ca"
    val updatedAt = "ua"
    val metadata = "me"
  }

  implicit val gameBSONHandler = new BSON[Game] {

    import BSONFields._

    def reads(r: BSON.Reader): Game = {
      val players = r.get[BSONArray]("p")
      Game(
        id = r str "_id",
        token = r str "tk",
        whitePlayer = players.getAs[Color ⇒ Player](0).get(White),
        blackPlayer = players.getAs[Color ⇒ Player](1).get(Black),
        binaryPieces = r bytes binaryPieces,
        status = Status(r int status) err "Invalid status",
        turns = r int turns,
        clock = ???,
        check = r strO check flatMap Pos.posAt,
        creatorColor = Color(r boolD creatorColor),
        positionHashes = r strD positionHashes,
        castleLastMoveTime = r.get[CastleLastMoveTime](castleLastMoveTime)(castleLastMoveTimeBSONHandler),
        mode = Mode(r boolD rated),
        variant = Variant(r intD variant) err "Invalid variant",
        next = r strO next,
        bookmarks = r intD bookmarks,
        createdAt = r date createdAt,
        updatedAt = r dateO updatedAt,
        metadata = r.getO[Metadata](metadata))
    }

    def writes(w: BSON.Writer, o: Game) = BSONDocument(
      id -> o.id,
      token -> o.token,
      players -> List(
        (_: Color) ⇒ o.whitePlayer,
        (_: Color) ⇒ o.blackPlayer),
      binaryPieces -> o.binaryPieces,
      status -> o.status.id,
      turns -> o.turns,
      clock -> ???,
      check -> o.check.map(_.toString),
      creatorColor -> w.boolO(o.creatorColor.white),
      positionHashes -> w.strO(o.positionHashes),
      castleLastMoveTime -> castleLastMoveTimeBSONHandler.write(o.castleLastMoveTime),
      rated -> w.boolO(o.mode.rated),
      variant -> w.intO(o.variant.id),
      next -> o.next,
      bookmarks -> w.intO(o.bookmarks),
      createdAt -> w.date(o.createdAt),
      updatedAt -> o.updatedAt.map(w.date),
      metadata -> o.metadata.map(metadataBSONHandler.write))
  }
}

case class CastleLastMoveTime(
    castles: Castles,
    lastMove: Option[(Pos, Pos)],
    lastMoveTime: Option[Int]) {

  def lastMoveString = lastMove map { case (a, b) ⇒ a.toString + b.toString }
}

object CastleLastMoveTime {

  def init = CastleLastMoveTime(Castles.all, None, None)

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
