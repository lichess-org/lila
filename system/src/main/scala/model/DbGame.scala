package lila.system
package model

import lila.chess._
import Pos.{ posAt, piotr }
import Role.forsyth

case class DbGame(
    id: String,
    players: List[DbPlayer],
    pgn: String,
    status: Int,
    turns: Int,
    clock: Option[Clock],
    lastMove: Option[String],
    positionHashes: String = "",
    castles: String = "KQkq",
    isRated: Boolean = false) {

  def playerById(id: String): Option[DbPlayer] = playersById get id

  def playerByColor(color: Color): Option[DbPlayer] = playersByColor get color

  def player: DbPlayer = playerByColor(if (0 == turns % 2) White else Black) get

  lazy val playersByColor: Map[Color, DbPlayer] = players map { p ⇒ (p.color, p) } toMap
  lazy val playersById: Map[String, DbPlayer] = players map { p ⇒ (p.id, p) } toMap

  def fullIdOf(player: DbPlayer): Option[String] =
    (players contains player) option id + player.id

  def toChess: Game = {

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

  private def toChessHistory = History(
    lastMove = lastMove flatMap {
      case MoveString(a, b) ⇒ for (o ← posAt(a); d ← posAt(b)) yield (o, d)
      case _                ⇒ None
    },
    castles = Map(
      White -> (castles contains 'K', castles contains 'Q'),
      Black -> (castles contains 'k', castles contains 'q')
    ),
    positionHashes = positionHashes grouped History.hashSize toList
  )

  def update(game: Game, move: Move): DbGame = {
    val allPieces = (game.board.pieces map {
      case (pos, piece) ⇒ (pos, piece, false)
    }) ++ (game.deads map {
      case (pos, piece) ⇒ (pos, piece, true)
    })
    val (history, situation) = (game.board.history, game.situation)
    val events = (Event fromMove move) ::: (Event fromSituation game.situation)
    copy(
      pgn = game.pgnMoves,
      players = for {
        player ← players
      } yield player.copy(
        ps = player encodePieces allPieces,
        evts = player.newEvts(events :+ Event.possibleMoves(game.situation, player.color))
      ),
      turns = game.turns,
      positionHashes = history.positionHashes mkString,
      castles = List(
        if (history canCastle White on KingSide) "K" else "",
        if (history canCastle White on QueenSide) "Q" else "",
        if (history canCastle Black on KingSide) "k" else "",
        if (history canCastle Black on QueenSide) "q" else ""
      ) mkString,
      status =
        if (situation.checkMate) DbGame.MATE
        else if (situation.staleMate) DbGame.STALEMATE
        else if (situation.autoDraw) DbGame.DRAW
        else status,
      clock = game.clock
    )
  }

  def playable = status < DbGame.ABORTED
}

object DbGame {

  val gameIdSize = 8
  val playerIdSize = 4
  val fullIdSize = 12

  val CREATED = 10
  val STARTED = 20
  val ABORTED = 25
  val MATE = 30
  val RESIGN = 31
  val STALEMATE = 32
  val TIMEOUT = 33
  val DRAW = 34
  val OUTOFTIME = 35
  val CHEAT = 36
}
