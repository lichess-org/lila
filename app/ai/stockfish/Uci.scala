package lila
package ai.stockfish

import chess.Pos

object Uci {

  def parseMove(move: String): Option[(Pos, Pos)] = for {
    orig ← Pos.posAt(move take 2)
    dest ← Pos.posAt(move drop 2)
  } yield orig -> dest
}
