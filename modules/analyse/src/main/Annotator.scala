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
            comment = advice.makeComment(true).some,
            variation = makeVariation(turn, advice)
          )
        )
      )
    }

  private def makeVariation(turn: Turn, advice: Advice): List[Turn] = {
    Turn.fromMoves(
      advice.info.variation map { san ⇒ Move(san) },
      turn plyOf advice.color
    ).reverse match {
        case Nil ⇒ Nil
        case turn :: turns ⇒ turn.updateLast {
          _.copy(comment = advice.prev.score map { score ⇒ s"(${score.showPawns})" })
        } :: turns
      }
  }.reverse
}
