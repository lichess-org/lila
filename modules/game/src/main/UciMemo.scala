package lila.game

import scala.concurrent.duration._

import chess.format.UciDump

final class UciMemo(ttl: Duration) {

  private val memo = lila.memo.Builder.expiry[String, Vector[String]](ttl)

  def add(game: Game, uciMove: String) {
    val current = Option(memo getIfPresent game.id) | Vector.empty
    memo.put(game.id, current :+ uciMove)
  }
  def add(game: Game, move: chess.Move) {
    add(game, UciDump.move(game.variant)(move))
  }

  def set(game: Game, uciMoves: Seq[String]) {
    memo.put(game.id, uciMoves.toVector)
  }

  def get(game: Game): Fu[Vector[String]] =
    Option(memo getIfPresent game.id) filter { moves =>
        moves.size == game.pgnMoves.size
      } match {
        case Some(moves) => fuccess(moves)
        case _           => compute(game) addEffect { set(game, _) }
      }

  def drop(game: Game, nb: Int) {
    val current = Option(memo getIfPresent game.id) | Vector.empty
    memo.put(game.id, current.take(current.size - nb))
  }

  private def compute(game: Game): Fu[Vector[String]] = for {
    fen ← game.variant.exotic ?? { GameRepo initialFen game.id }
    uciMoves ← UciDump(game.pgnMoves, fen, game.variant).future
  } yield uciMoves.toVector
}
