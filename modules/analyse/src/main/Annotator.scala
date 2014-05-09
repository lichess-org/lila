package lila.analyse

import chess.format.pgn.{ Pgn, Tag, Turn, Move }
import chess.OpeningExplorer.Opening

private[analyse] final class Annotator(netDomain: String) {

  def apply(p: Pgn, analysis: Option[Analysis], opening: Option[Opening]): Pgn =
    annotateOpening(opening) {
      annotateTurns(p, analysis ?? (_.advices))
    }.copy(tags = p.tags :+ Tag("Annotator", netDomain))

  private def annotateOpening(opening: Option[Opening])(p: Pgn) = opening.fold(p) { o =>
    p.updatePly(o.size, _.copy(opening = o.name.some))
  }

  private def annotateTurns(p: Pgn, advices: List[Advice]): Pgn =
    advices.foldLeft(p) {
      case (pgn, advice) => pgn.updateTurn(advice.turn, turn =>
        turn.update(advice.color, move =>
          move.copy(
            nag = advice.nag.code.some,
            comment = advice.makeComment(true, true).some,
            variation = makeVariation(turn, advice)
          )
        )
      )
    }

  private def makeVariation(turn: Turn, advice: Advice): List[Turn] =
    Turn.fromMoves(
      advice.info.variation take 16 map { san => Move(san) },
      turn plyOf advice.color
    )
}
