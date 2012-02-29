package lila.system
package model

import com.novus.salat.annotations._
import com.mongodb.casbah.Imports._

import lila.chess._
import Pos._

case class DbGame(
    @Key("_id") id: String,
    players: List[Player],
    pgn: String,
    status: Int,
    turns: Int,
    variant: Int) {

  def toChess = Game(
    board = Board(Map(A1 -> White.rook), History()),
    player = if (0 == turns % 2) White else Black,
    pgnMoves = pgn
  )
}
