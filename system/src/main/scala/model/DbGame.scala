package lila.system
package model

import com.novus.salat.annotations._
import com.mongodb.casbah.Imports._

import lila.chess._
import Pos.posAt

case class DbGame(
    @Key("_id") id: String,
    players: List[DbPlayer],
    pgn: String,
    status: Int,
    turns: Int,
    clock: Option[DbClock],
    lastMove: Option[String]) {

  def playerById(id: String): Option[DbPlayer] = playersById get id

  def playerByColor(color: String): Option[DbPlayer] = playersByColor get color

  lazy val playersByColor: Map[String, DbPlayer] = players map { p ⇒ (p.color, p) } toMap
  lazy val playersById: Map[String, DbPlayer] = players map { p ⇒ (p.id, p) } toMap

  def fullIdOf(player: DbPlayer): Option[String] =
    (players contains player) option id + player.id

  def toChess: Game = {

    def posPiece(posCode: Char, roleCode: Char, color: Color): Option[(Pos, Piece)] = for {
      pos ← Piotr.decodePos get posCode
      role ← Piotr.decodeRole get roleCode
    } yield (pos, Piece(color, role))

    val (pieces, deads) = {
      for {
        player ← players
        color = Color.allByName(player.color) // unsafe
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
      board = Board(
        pieces,
        History(
          lastMove = lastMove flatMap {
            case MoveString(a, b) ⇒ for (from ← posAt(a); to ← posAt(b)) yield (from, to)
            case _              ⇒ None
          }
        )
      ),
      player = if (0 == turns % 2) White else Black,
      pgnMoves = pgn,
      clock = for {
        c ← clock
        color ← Color(c.color)
        whiteTime ← c.times get "white"
        blackTime ← c.times get "black"
      } yield Clock(
        color = color,
        increment = c.increment,
        limit = c.limit,
        times = Map(White -> whiteTime, Black -> blackTime)
      ),
      deads = deads,
      turns = turns
    )
  }

  def update(game: Game): DbGame = {
    val allPieces = (game.board.pieces map {
      case (pos, piece) ⇒ (pos, piece, false)
    }) ++ (game.deads map {
      case (pos, piece) ⇒ (pos, piece, true)
    })
    copy(
      pgn = game.pgnMoves,
      players = for {
        player ← players
        color = Color.allByName(player.color) // unsafe
      } yield player.copy(
        ps = allPieces filter (_._2.color == color) map {
          case (pos, piece, dead) ⇒ (Piotr encodePos pos).toString + {
            if (dead) piece.role.forsyth.toUpper
            else piece.role.forsyth
          }
        } mkString " "
      ),
      turns = game.turns
    )
  }
}

object DbGame {

  val gameIdSize = 8
  val playerIdSize = 4
  val fullIdSize = 12
}
