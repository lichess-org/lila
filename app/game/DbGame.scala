package lila
package game

import round.{ Event, Progress }
import user.User
import chess.{ History ⇒ ChessHistory, Role, Board, Move, Pos, Game, Clock, Status, Color, Piece, Variant, Mode }
import Color._
import chess.Pos.piotr
import chess.Role.forsyth

import org.joda.time.DateTime
import org.scala_tools.time.Imports._
import com.novus.salat.annotations.Key
import scala.math.min

case class DbGame(
    id: String,
    whitePlayer: DbPlayer,
    blackPlayer: DbPlayer,
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
    is960Rematch: Boolean = false,
    createdAt: DateTime = DateTime.now,
    updatedAt: Option[DateTime] = None,
    tournamentId: Option[String] = None) {

  val players = List(whitePlayer, blackPlayer)

  val playersByColor: Map[Color, DbPlayer] = Map(
    White -> whitePlayer,
    Black -> blackPlayer
  )

  def player(color: Color): DbPlayer = color match {
    case White ⇒ whitePlayer
    case Black ⇒ blackPlayer
  }

  def player(playerId: String): Option[DbPlayer] =
    players find (_.id == playerId)

  def player(user: User): Option[DbPlayer] =
    players find (_ isUser user)

  def player(c: Color.type ⇒ Color): DbPlayer = player(c(Color))

  def isPlayerFullId(player: DbPlayer, fullId: String): Boolean =
    (fullId.size == DbGame.fullIdSize) && player.id == (fullId drop 8)

  def player: DbPlayer = player(turnColor)

  def opponent(p: DbPlayer): DbPlayer = opponent(p.color)

  def opponent(c: Color): DbPlayer = player(!c)

  def turnColor = Color(0 == turns % 2)

  def turnOf(p: DbPlayer) = p == player

  def fullIdOf(player: DbPlayer): Option[String] =
    (players contains player) option id + player.id

  def fullIdOf(color: Color): String = id + player(color).id

  def isTournament = tournamentId.isDefined
  def nonTournament = tournamentId.isEmpty

  def hasChat = nonTournament && nonAi

  lazy val toChess: Game = {

    def posPiece(posCode: Char, roleCode: Char, color: Color): Option[(Pos, Piece)] = for {
      pos ← piotr(posCode)
      role ← forsyth(roleCode)
    } yield (pos, Piece(color, role))

    val (pieces, deads) = {
      for {
        player ← players
        color = player.color
        piece ← player.ps grouped 2
      } yield (color, piece(0), piece(1))
    }.foldLeft((Map[Pos, Piece](), List[(Pos, Piece)]())) {
      case ((ps, ds), (color, pos, role)) ⇒ {
        if (role.isUpper) posPiece(pos, role.toLower, color) map { p ⇒ (ps, p :: ds) }
        else posPiece(pos, role, color) map { p ⇒ (ps + p, ds) }
      } getOrElse (ps, ds)
      case (acc, _) ⇒ acc
    }

    Game(
      board = Board(pieces, toChessHistory, variant),
      player = Color(0 == turns % 2),
      clock = clock,
      deads = deads,
      turns = turns
    )
  }

  lazy val toChessHistory = ChessHistory(
    lastMove = lastMove,
    castles = castles,
    positionHashes = positionHashes)

  def update(
    game: Game,
    move: Move,
    blur: Boolean = false): (Progress, String) = {
    val (history, situation) = (game.board.history, game.situation)
    val events =
      Event.possibleMoves(game.situation, White) ::
        Event.possibleMoves(game.situation, Black) ::
        Event.State(game.situation.color, game.turns) ::
        (Event fromMove move) :::
        (Event fromSituation game.situation)

    def copyPlayer(player: DbPlayer) = player.copy(
      ps = player encodePieces game.allPieces,
      blurs = player.blurs + (blur && move.color == player.color).fold(1, 0),
      moveTimes = (move.color == player.color).fold(
        lastMoveTime.fold(
          lmt ⇒ (nowSeconds - lmt) |> { mt ⇒
            player.moveTimes.isEmpty.fold(
              mt.toString,
              player.moveTimes + " " + mt
            )
          },
          ""
        ),
        player.moveTimes)
    )

    val updated = copy(
      whitePlayer = copyPlayer(whitePlayer),
      blackPlayer = copyPlayer(blackPlayer),
      turns = game.turns,
      positionHashes = history.positionHashes mkString,
      castles = history.castleNotation,
      lastMove = history.lastMoveString,
      status =
        if (situation.checkMate) Status.Mate
        else if (situation.staleMate) Status.Stalemate
        else if (situation.autoDraw) Status.Draw
        else status,
      clock = game.clock,
      check = if (situation.check) situation.kingPos else None,
      lastMoveTime = nowSeconds.some
    )

    val finalEvents = events :::
      updated.clock.fold(c ⇒ List(Event.Clock(c)), Nil) ::: {
        (updated.playable && (
          abortable != updated.abortable || (Color.all exists { color ⇒
            playerCanOfferDraw(color) != updated.playerCanOfferDraw(color)
          })
        )).fold(Color.all map Event.ReloadTable, Nil)
      }

    Progress(this, updated, finalEvents) -> game.pgnMoves
  }

  def updatePlayer(color: Color, f: DbPlayer ⇒ DbPlayer) = color match {
    case White ⇒ copy(whitePlayer = f(whitePlayer))
    case Black ⇒ copy(blackPlayer = f(blackPlayer))
  }

  def updatePlayers(f: DbPlayer ⇒ DbPlayer) = copy(
    whitePlayer = f(whitePlayer),
    blackPlayer = f(blackPlayer)
  )

  def start = started.fold(this, copy(
    status = Status.Started,
    mode = Mode(mode.rated && (players forall (_.hasUser))),
    updatedAt = DateTime.now.some
  ))

  def startClock(compensation: Float) = clock filterNot (_.isRunning) fold (
    c ⇒ copy(clock = c.run.giveTime(creatorColor, compensation).some),
    this
  )

  def hasMoveTimes = players forall (_.hasMoveTimes)

  def started = status >= Status.Started

  def notStarted = !started

  def aborted = status == Status.Aborted

  def playable = status < Status.Aborted

  def playableBy(p: DbPlayer): Boolean = playable && turnOf(p)

  def playableBy(c: Color): Boolean = playableBy(player(c))

  def aiLevel: Option[Int] = players find (_.isAi) flatMap (_.aiLevel)

  def hasAi: Boolean = players exists (_.isAi)
  def nonAi = !hasAi

  def mapPlayers(f: DbPlayer ⇒ DbPlayer) = copy(
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
    player(color).lastDrawOffer.fold(_ >= turns - 1, false)

  def playerCanRematch(color: Color) =
    finishedOrAborted && opponent(color).isHuman && nonTournament

  def playerCanProposeTakeback(color: Color) =
    started && playable && nonTournament &&
      bothPlayersHaveMoved &&
      !opponent(color).isProposingTakeback

  def moretimeable = playable && nonTournament && hasClock

  def abortable = status == Status.Started && turns < 2 && nonTournament

  def resignable = playable && !abortable

  def finish(status: Status, winner: Option[Color]) = Progress(
    this,
    copy(
      status = status,
      whitePlayer = whitePlayer finish (winner == Some(White)),
      blackPlayer = blackPlayer finish (winner == Some(Black)),
      clock = clock map (_.stop)
    ),
    List(Event.End())
  )

  def rated = mode.rated

  def finished = status >= Status.Mate

  def finishedOrAborted = finished || aborted

  def winner = players find (_.wins)

  def loser = winner map opponent

  def winnerColor: Option[Color] = winner map (_.color)

  def winnerUserId: Option[String] = winner flatMap (_.userId)

  def wonBy(c: Color): Option[Boolean] = winnerColor map (_ == c)

  def outoftimePlayer: Option[DbPlayer] = for {
    c ← clock
    if playable
    if !c.isRunning || (c outoftime player.color)
  } yield player

  def hasClock = clock.isDefined

  def isClockRunning = clock.fold(_.isRunning, false)

  def withClock(c: Clock) = Progress(this, copy(clock = Some(c)))

  def estimateTotalTime = clock.fold(
    c ⇒ c.limit + 30 * c.increment,
    1200 // default to 20 minutes
  )

  def creator = player(creatorColor)

  def invitedColor = !creatorColor

  def invited = player(invitedColor)

  def playerWhoDidNotMove: Option[DbPlayer] = turns match {
    case 0 ⇒ player(White).some
    case 1 ⇒ player(Black).some
    case _ ⇒ none
  }

  def bothPlayersHaveMoved = turns > 1

  def playerMoves(color: Color): Int = (turns + color.fold(1, 0)) / 2

  def playerHasMoved(color: Color) = playerMoves(color) > 0

  def playerBlurPercent(color: Color): Int = (turns > 5).fold(
    (player(color).blurs * 100) / playerMoves(color),
    0
  )

  def deadPiecesOf(color: Color): List[Role] = toChess.deads collect {
    case (_, piece) if piece is color ⇒ piece.role
  }

  def isBeingPlayed =
    !finishedOrAborted && updatedAt.fold(_ > DateTime.now - 20.seconds, false)

  def abandoned = updatedAt.fold(
    u ⇒ (status <= Status.Started) && (u <= DbGame.abandonedDate),
    false
  )

  def hasBookmarks = bookmarks > 0

  def showBookmarks = if (hasBookmarks) bookmarks else ""

  def encode = RawDbGame(
    id = id,
    p = players map (_.encode),
    s = status.id,
    t = turns,
    c = clock map RawDbClock.encode,
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
    r960 = is960Rematch option true,
    ca = createdAt,
    ua = updatedAt,
    tid = tournamentId
  )

  def userIds = playerMaps(_.userId)

  def userElos = playerMaps(_.elo)

  def averageUsersElo = userElos match {
    case a :: b :: Nil ⇒ Some((a + b) / 2)
    case a :: Nil      ⇒ Some((a + 1200) / 2)
    case Nil           ⇒ None
  }

  def with960Rematch(v: Boolean) = this.copy(is960Rematch = v)

  def withTournamentId(id: String) = this.copy(tournamentId = id.some)

  def withId(newId: String) = this.copy(id = newId)

  private def playerMaps[A](f: DbPlayer ⇒ Option[A]): List[A] = players.map(f).flatten
}

object DbGame {

  val gameIdSize = 8
  val playerIdSize = 4
  val fullIdSize = 12

  def abandonedDate = DateTime.now - 10.days

  def takeGameId(fullId: String) = fullId take gameIdSize

  def apply(
    game: Game,
    whitePlayer: DbPlayer,
    blackPlayer: DbPlayer,
    ai: Option[(Color, Int)],
    creatorColor: Color,
    mode: Mode,
    variant: Variant): DbGame = DbGame(
    id = IdGenerator.game,
    whitePlayer = whitePlayer withEncodedPieces game.allPieces,
    blackPlayer = blackPlayer withEncodedPieces game.allPieces,
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
    createdAt = DateTime.now)
}

case class RawDbGame(
    @Key("_id") id: String,
    p: List[RawDbPlayer],
    s: Int,
    t: Int,
    c: Option[RawDbClock],
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
    r960: Option[Boolean] = None,
    ca: DateTime,
    ua: Option[DateTime],
    tid: Option[String]) {

  def decode: Option[DbGame] = for {
    whitePlayer ← p.headOption map (_ decode Color.White)
    blackPlayer ← p lift 1 map (_ decode Color.Black)
    trueStatus ← Status(s)
  } yield DbGame(
    id = id,
    whitePlayer = whitePlayer,
    blackPlayer = blackPlayer,
    status = trueStatus,
    turns = t,
    clock = c map (_.decode),
    lastMove = lm,
    check = ck flatMap Pos.posAt,
    creatorColor = cc.fold(Color.apply, Color.White),
    positionHashes = ph | "",
    castles = cs | "-",
    mode = (ra map Mode.apply) | Mode.Casual,
    variant = (v flatMap Variant.apply) | Variant.Standard,
    next = next,
    lastMoveTime = lmt,
    bookmarks = bm | 0,
    is960Rematch = r960 | false,
    createdAt = ca,
    updatedAt = ua,
    tournamentId = tid
  )
}
