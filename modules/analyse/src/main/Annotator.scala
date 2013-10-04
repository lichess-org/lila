package lila.analyse

import chess.format.pgn.{ Pgn, Tag, Turn, Move }

private[analyse] final class Annotator(netDomain: String) {

  def apply(p: Pgn, analysis: Analysis): Pgn =
    annotateTurns(p, analysis.advices).copy(
      tags = p.tags :+ Tag("Annotator", netDomain)
    )

  private def annotateTurns(p: Pgn, advices: List[Advice]): Pgn =
    advices.foldLeft(p) {
      case (pgn, advice) ⇒ pgn.updateTurn(advice.turn, turn ⇒
        turn.update(advice.color, move ⇒
          move.copy(
            nag = advice.nag.code.some,
            comment = makeComment(advice).some,
            variation = advice.info.line
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
