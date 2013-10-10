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
            comment = makeComment(advice),
            variation = advice.info.variation
          )
        )
      )
    }

  private def makeComment(advice: Advice): Option[String] = (advice match {
    case MateAdvice(sev, _, _) ⇒ sev.desc.some
    case _                     ⇒ none
  })

  // private def makeBestComment(advice: Advice): Option[String] =
  //   (advice.info.move != advice.info.best) option {
  //     "Best was " format advice.info.best.uci
  //   }
}
