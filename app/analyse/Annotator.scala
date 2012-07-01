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
          move.copy(
            nag = advice.nag.code.some,
            comment = makeComment(advice).some
          )
        )
      )
    }

  private def makeComment(advice: Advice): String = advice match {
    case CpAdvice(sev, info, _, _)   ⇒ "%s. Best was %s".format(sev.nag, info.showBest)
    case MateAdvice(sev, info, _, _) ⇒ "%s. Best was %s".format(sev.desc, info.showBest)
  }
}
