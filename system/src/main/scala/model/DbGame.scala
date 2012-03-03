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

  def toChess = {

    def posPiece(posCode: Char, roleCode: Char, color: Color): Option[(Pos, Piece)] = for {
      pos ← Piotr.decodePos get posCode
      role ← Piotr.decodeRole get roleCode
    } yield (pos, Piece(color, role))

    val LastMove = """^([a-h][1-8]) ([a-h][1-8])$""".r

    Game(
      board = Board(
        (for {
          player ← players
          color = Color.allByName(player.color)
          piece ← player.ps.split(' ').toList
        } yield piece.toList match {
          case pos :: role :: Nil  ⇒ posPiece(pos, role, color)
          case pos :: role :: rest ⇒ posPiece(pos, role, color)
          case _                   ⇒ None
        }).flatten toMap,
        History(
          lastMove = lastMove flatMap {
            case LastMove(a, b) ⇒ for (from ← posAt(a); to ← posAt(b)) yield (from, to)
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
      )
    )
  }
}
