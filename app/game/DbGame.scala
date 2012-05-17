package lila
package game

import round.{ Event, Progress }
import user.User
import chess.{ History ⇒ ChessHistory, Board, Move, Pos, Game, Clock, Status, Color, Piece, Variant }
import Color._
import chess.format.{ PgnReader, Fen }
import chess.Pos.piotr
import chess.Role.forsyth

import org.joda.time.DateTime

case class DbGame(
    id: String,
    whitePlayer: DbPlayer,
    blackPlayer: DbPlayer,
    pgn: String,
    status: Status,
    turns: Int,
    clock: Option[Clock],
    lastMove: Option[String],
    check: Option[Pos] = None,
    creatorColor: Color,
    positionHashes: String = "",
    castles: String = "KQkq",
    isRated: Boolean = false,
    variant: Variant = Variant.default,
    lastMoveTime: Option[Int] = None,
    createdAt: Option[DateTime] = None) {

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

  def isPlayerFullId(player: DbPlayer, fullId: String): Boolean =
    (fullId.size == DbGame.fullIdSize) && player.id == (fullId drop 8)

  def opponent(p: DbPlayer): DbPlayer = player(!(p.color))

  def player: DbPlayer = player(if (0 == turns % 2) White else Black)

  def fullIdOf(player: DbPlayer): Option[String] =
    (players contains player) option id + player.id

  def fullIdOf(color: Color): String = id + player(color).id

  lazy val toChess: Game = {

    def posPiece(posCode: Char, roleCode: Char, color: Color): Option[(Pos, Piece)] = for {
      pos ← piotr(posCode)
      role ← forsyth(roleCode)
    } yield (pos, Piece(color, role))

    val (pieces, deads) = {
      for {
        player ← players
        color = player.color
        piece ← player.ps.split(' ')
      } yield (color, piece(0), piece(1))
    }.foldLeft((Map[Pos, Piece](), List[(Pos, Piece)]())) {
      case ((ps, ds), (color, pos, role)) ⇒ {
        if (role.isUpper) posPiece(pos, role.toLower, color) map { p ⇒ (ps, p :: ds) }
        else posPiece(pos, role, color) map { p ⇒ (ps + p, ds) }
      } getOrElse (ps, ds)
      case (acc, _) ⇒ acc
    }

    Game(
      board = Board(pieces, toChessHistory),
      player = if (0 == turns % 2) White else Black,
      pgnMoves = pgn,
      clock = clock,
      deads = deads,
      turns = turns
    )
  }

  def toChessHistory = ChessHistory(
    lastMove = lastMove,
    castles = castles,
    positionHashes = positionHashes)

  def update(game: Game, move: Move, blur: Boolean = false): Progress = {
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
      moveTimes = (recordMoveTimes && move.color == player.color).fold(
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
      pgn = game.pgnMoves,
      whitePlayer = copyPlayer(whitePlayer),
      blackPlayer = copyPlayer(blackPlayer),
      turns = game.turns,
      positionHashes = history.positionHashes mkString,
      castles = history.castleNotation,
      lastMove = history.lastMove map { case (a, b) ⇒ a + " " + b },
      status =
        if (situation.checkMate) Status.Mate
        else if (situation.staleMate) Status.Stalemate
        else if (situation.autoDraw) Status.Draw
        else status,
      clock = game.clock,
      check = if (situation.check) situation.kingPos else None,
      lastMoveTime = recordMoveTimes option nowSeconds
    )

    val finalEvents = events :::
      updated.clock.fold(c ⇒ List(Event.Clock(c)), Nil) ::: {
        (updated.playable && (
          abortable != updated.abortable || (Color.all exists { color ⇒
            playerCanOfferDraw(color) != updated.playerCanOfferDraw(color)
          })
        )).fold(Color.all map Event.ReloadTable, Nil)
      }

    Progress(this, updated, finalEvents)
  }

  def rewind(initialFen: Option[String]): Valid[Progress] = {
    PgnReader.withSans(
      pgn = pgn,
      op = _.init,
      tags = initialFen.fold(fen ⇒ List(Fen(fen)), Nil)
    ) map { replay ⇒
        val rewindedGame = replay.game
        val rewindedHistory = rewindedGame.board.history
        val rewindedSituation = rewindedGame.situation
        def rewindPlayer(player: DbPlayer) = player.copy(
          ps = player encodePieces rewindedGame.allPieces,
          isProposingTakeback = false)
        Progress(this, copy(
          pgn = rewindedGame.pgnMoves,
          whitePlayer = rewindPlayer(whitePlayer),
          blackPlayer = rewindPlayer(blackPlayer),
          turns = rewindedGame.turns,
          positionHashes = rewindedHistory.positionHashes mkString,
          castles = rewindedHistory.castleNotation,
          lastMove = rewindedHistory.lastMove map { case (a, b) ⇒ a + " " + b },
          status =
            if (rewindedSituation.checkMate) Status.Mate
            else if (rewindedSituation.staleMate) Status.Stalemate
            else if (rewindedSituation.autoDraw) Status.Draw
            else status,
          clock = clock map (_.switch),
          check = if (rewindedSituation.check) rewindedSituation.kingPos else None,
          lastMoveTime = recordMoveTimes option nowSeconds
        ))
      }
  }

  def updatePlayer(color: Color, f: DbPlayer ⇒ DbPlayer) = color match {
    case White ⇒ copy(whitePlayer = f(whitePlayer))
    case Black ⇒ copy(blackPlayer = f(blackPlayer))
  }

  def updatePlayers(f: DbPlayer ⇒ DbPlayer) = copy(
    whitePlayer = f(whitePlayer),
    blackPlayer = f(blackPlayer)
  )

  def recordMoveTimes = !hasAi

  def hasMoveTimes = players forall (_.hasMoveTimes)

  def playable = status < Status.Aborted

  def playableBy(p: DbPlayer) = playable && p == player

  def aiLevel: Option[Int] = players find (_.isAi) flatMap (_.aiLevel)

  lazy val hasAi: Boolean = players exists (_.isAi)

  def mapPlayers(f: DbPlayer ⇒ DbPlayer) = copy(
    whitePlayer = f(whitePlayer),
    blackPlayer = f(blackPlayer)
  )

  def playerCanOfferDraw(color: Color) =
    status >= Status.Started &&
      status < Status.Aborted &&
      turns >= 2 &&
      !player(color).isOfferingDraw &&
      !(player(!color).isAi) &&
      !(playerHasOfferedDraw(color))

  def playerHasOfferedDraw(color: Color) =
    player(color).lastDrawOffer some (_ >= turns - 1) none false

  def abortable = status == Status.Started && turns < 2

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

  def rated = isRated

  def finished = status >= Status.Mate

  def winnerColor: Option[Color] = players find (_.wins) map (_.color)

  def outoftimePlayer: Option[DbPlayer] = for {
    c ← clock
    if this.playable
    if !c.isRunning || (c outoftime player.color)
  } yield player

  def hasClock = clock.isDefined

  def withClock(c: Clock) = Progress(this, copy(clock = Some(c)))

  def creator = player(creatorColor)

  def invited = player(!creatorColor)

  def pgnList = pgn.split(' ').toList

  def bothPlayersHaveMoved = turns > 1
}

object DbGame {

  val gameIdSize = 8
  val playerIdSize = 4
  val fullIdSize = 12

  def takeGameId(fullId: String) = fullId take gameIdSize

  def apply(
    game: Game, 
    whitePlayer: DbPlayer,
    blackPlayer: DbPlayer,
    ai: Option[(Color, Int)],
    creatorColor: Color,
    isRated: Boolean,
    variant: Variant,
    createdAt: DateTime): DbGame = DbGame(
    id = IdGenerator.game,
    whitePlayer = whitePlayer withEncodedPieces game.allPieces,
    blackPlayer = blackPlayer withEncodedPieces game.allPieces,
    pgn = "",
    status = Status.Created,
    turns = game.turns,
    clock = game.clock,
    lastMove = None,
    check = None,
    creatorColor = creatorColor,
    positionHashes = "",
    castles = "KQkq",
    isRated = isRated,
    variant = variant,
    lastMoveTime = None,
    createdAt = createdAt.some) 
}
