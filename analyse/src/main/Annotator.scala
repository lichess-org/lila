package lila.analyse

import chess.format.pgn

final class Annotator(netDomain: String) {

  def apply(p: pgn.Pgn, analysis: Analysis): pgn.Pgn =
    annotateTurns(p, analysis.advices).copy(
      tags = p.tags :+ pgn.Tag("Annotator", netDomain)
    )

  private def annotateTurns(p: pgn.Pgn, advices: List[Advice]): pgn.Pgn =
    advices.foldLeft(p) {
      case (acc, advice) ⇒ acc.updateTurn(advice.turn, turn ⇒
        turn.update(advice.color, move ⇒
          move.copy(
            nag = advice.nag.code.some,
            comment = makeComment(advice).some
          )
        )
      )
    }

  private def makeComment(advice: Advice): String = (advice match {
    case CpAdvice(sev, _, _)   ⇒ sev.nag.toString
    case MateAdvice(sev, _, _) ⇒ sev.desc
  }) ++ makeBestComment(advice).fold(".")(". " + _)

  private def makeBestComment(advice: Advice): Option[String] = 
    (advice.info.move != advice.info.best) option {
      "Best was %s." format advice.info.best.uci
    }
}
