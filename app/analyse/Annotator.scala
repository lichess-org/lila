package lila
package analyse

import chess.format.pgn

object Annotator {

  def apply(p: pgn.Pgn, analysis: Analysis): pgn.Pgn = 
    annotateTurns(p, analysis.advices).copy(
    tags = p.tags :+ pgn.Tag("Annotator", "lichess.org")
  )

  private def annotateTurns(p: pgn.Pgn, advices: List[Advice]): pgn.Pgn =
    advices.foldLeft(p) {
      case (acc, advice) ⇒ acc.updateTurn(advice.fullMove, turn ⇒
        turn.update(advice.color, move ⇒
          move.copy(nag = advice.nag.code.some)
        )
      )
    }
}
