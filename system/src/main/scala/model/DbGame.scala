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

  def playerById(id: String) = playersById get id

  def playerByColor(color: String) = playersByColor get color

  lazy val playersByColor: Map[String, DbPlayer] = players map { p ⇒ (p.color, p) } toMap
  lazy val playersById: Map[String, DbPlayer] = players map { p ⇒ (p.id, p) } toMap

  def fullIdOf(player: DbPlayer): Option[String] =
    (players contains player) option id + player.id

  def toChess: Game = {

    def posPiece(posCode: Char, roleCode: Char, color: Color): Option[(Pos, Piece)] = for {
      pos ← Piotr.decodePos get posCode
      role ← Piotr.decodeRole get roleCode
    } yield (pos, Piece(color, role))

    val LastMove = """^([a-h][1-8]) ([a-h][1-8])$""".r

    val (pieces, deads) = {
      for {
        player ← players
        color = Color.allByName(player.color) // unsafe
        piece ← player.ps.split(' ')
      } yield (color, piece(0), piece(1))
    }.foldLeft((Map.empty: Map[Pos, Piece], Map.empty: Map[Pos, Piece])) {
      case ((ps, ds), (color, pos, role)) ⇒ {
        if (role.isUpper) posPiece(pos, role.toLower, color) map { p ⇒ (ps, ds + p) }
        else posPiece(pos, role, color) map { p ⇒ (ps + p, ds) }
      } getOrElse (ps, ds)
      case (acc, _) ⇒ acc
    }

    Game(
      board = Board(
        pieces,
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
      ),
      deads = deads
    )
  }

  def update(game: Game): DbGame = {
    copy(
      pgn = game.pgnMoves,
      players = for {
        player ← players
        color = Color.allByName(player.color) // unsafe
      } yield player.copy(
        ps = game.board actorsOf color map { actor ⇒
          (Piotr encodePos actor.pos).toString + actor.piece.role.forsyth
        } mkString " "
      )
    )
  }
}

object DbGame {

  val gameIdSize = 8
  val playerIdSize = 4
  val fullIdSize = 12
}
