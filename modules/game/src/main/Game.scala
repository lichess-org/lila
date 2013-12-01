package lila.game

import chess.Color._
import chess.Pos.piotr, chess.Role.forsyth
import chess.{ History ⇒ ChessHistory, Role, Board, Move, Pos, Game ⇒ ChessGame, Clock, Status, Color, Piece, Variant, Mode }
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
    lastMove: Option[String],
    check: Option[Pos] = None,
    creatorColor: Color,
    positionHashes: String = "",
    castles: String = "KQkq",
    mode: Mode = Mode.default,
    variant: Variant = Variant.default,
    next: Option[String] = None,
    lastMoveTime: Option[Int] = None,
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

    val (pieces, deads) = BinaryFormat.piece decode binaryPieces

    ChessGame(
      board = Board(pieces, toChessHistory, variant),
      player = Color(0 == turns % 2),
      clock = clock,
      deads = deads,
      turns = turns)
  }

  lazy val toChessHistory = ChessHistory(
    lastMove = lastMove,
    castles = castles,
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
        lastMoveTime ?? { lmt ⇒
          val mt = nowSeconds - lmt
          val encoded = MoveTime encode mt
          player.moveTimes.isEmpty.fold(encoded.toString, player.moveTimes + encoded)
        }, player.moveTimes
      )
    )

    val updated = copy(
      whitePlayer = copyPlayer(whitePlayer),
      blackPlayer = copyPlayer(blackPlayer),
      turns = game.turns,
      positionHashes = history.positionHashes.mkString,
      castles = history.castleNotation,
      lastMove = history.lastMoveString,
      status = situation.status | status,
      clock = game.clock,
      check = situation.kingPos ifTrue situation.check,
      lastMoveTime = nowSeconds.some)

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

  def encode = RawGame(
    id = id,
    tk = token.some filter (Game.defaultToken !=),
    p = players map (_.encode),
    ps = binaryPieces,
    s = status.id,
    t = turns,
    c = clock map RawClock.encode,
    lm = lastMove,
    ck = check map (_.key),
    cc = creatorColor.white.fold(None, Some(false)),
    ph = positionHashes.some filter (_.nonEmpty),
    cs = castles.some filter ("-" !=),
    ra = mode.rated option true,
    v = variant.exotic option variant.id,
    next = next,
    lmt = lastMoveTime,
    bm = bookmarks.some filter (0 <),
    ca = createdAt,
    ua = updatedAt,
    me = metadata map (_.encode))

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

  object ShortFields {
    val createdAt = "ca"
    val updatedAt = "ua"
  }

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
    binaryPieces = BinaryFormat.piece encode game.allPieces,
    status = Status.Created,
    turns = game.turns,
    clock = game.clock,
    lastMove = None,
    check = None,
    creatorColor = creatorColor,
    positionHashes = "",
    castles = "KQkq",
    mode = mode,
    variant = variant,
    lastMoveTime = None,
    metadata = Metadata(
      source = source.some,
      pgnImport = pgnImport,
      tournamentId = none,
      tvAt = none).some,
    createdAt = DateTime.now)

  import lila.db.Tube
  import play.api.libs.json._

  private[game] lazy val tube = Tube(
    Reads[Game](js ⇒
      ~(for {
        obj ← js.asOpt[JsObject]
        rawGame ← RawGame.tube.read(obj).asOpt
        game ← rawGame.decode
      } yield JsSuccess(game): JsResult[Game])
    ),
    Writes[Game](game ⇒
      RawGame.tube.write(game.encode) getOrElse JsUndefined("[db] Can't write game " + game.id)
    )
  )
}

private[game] case class RawGame(
    id: String,
    tk: Option[String] = None,
    p: List[RawPlayer],
    ps: ByteArray,
    s: Int,
    t: Int,
    c: Option[RawClock],
    lm: Option[String],
    ck: Option[String],
    cc: Option[Boolean] = None,
    ph: Option[String] = None,
    cs: Option[String] = None,
    ra: Option[Boolean] = None,
    v: Option[Int] = None,
    next: Option[String] = None,
    lmt: Option[Int] = None,
    bm: Option[Int] = None,
    ca: DateTime,
    ua: Option[DateTime],
    me: Option[RawMetadata]) {

  def decode: Option[Game] = for {
    whitePlayer ← p.headOption map (_ decode Color.White)
    blackPlayer ← p lift 1 map (_ decode Color.Black)
    trueStatus ← Status(s)
    metadata = me map (_.decode)
  } yield Game(
    id = id,
    token = tk | Game.defaultToken,
    whitePlayer = whitePlayer,
    blackPlayer = blackPlayer,
    binaryPieces = ps,
    status = trueStatus,
    turns = t,
    clock = c map (_.decode),
    lastMove = lm,
    check = ck flatMap Pos.posAt,
    creatorColor = cc.fold(Color.white)(Color.apply),
    positionHashes = ~ph,
    castles = cs | "-",
    mode = (ra map Mode.apply) | Mode.Casual,
    variant = (v flatMap Variant.apply) | Variant.Standard,
    next = next,
    lastMoveTime = lmt,
    bookmarks = ~bm,
    createdAt = ca,
    updatedAt = ua,
    metadata = me map (_.decode)
  )
}

private[game] object RawGame {

  import lila.db.Tube
  import Tube.Helpers._
  import play.api.libs.json._

  private implicit def playerTube = RawPlayer.tube
  private implicit def clockTube = RawClock.tube
  private implicit def metadataTube = RawMetadata.tube

  private def defaults = Json.obj(
    "tk" -> none[String],
    "c" -> none[RawClock],
    "lm" -> none[String],
    "ck" -> none[String],
    "cc" -> none[Boolean],
    "ph" -> none[String],
    "cs" -> none[String],
    "ra" -> none[Boolean],
    "v" -> none[Int],
    "next" -> none[String],
    "lmt" -> none[Int],
    "bm" -> none[Int],
    "me" -> none[RawMetadata],
    "ua" -> none[DateTime])

  private[game] lazy val tube = Tube(
    (__.json update (
      merge(defaults) andThen readDate('ca) andThen readDateOpt('ua)
    )) andThen Json.reads[RawGame],
    Json.writes[RawGame] andThen (__.json update (
      writeDate('ca) andThen writeDateOpt('ua)
    ))
  )
}
