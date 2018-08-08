package lidraughts.analyse

import draughts.format.pdn.{ Pdn, Tag, Turn, Move, Glyphs }
import draughts.opening._
import draughts.{ Status, Color, Clock }

private[analyse] final class Annotator(netDomain: String) {

  def apply(
    p: Pdn,
    analysis: Option[Analysis],
    opening: Option[FullOpening.AtPly],
    winner: Option[Color],
    status: Status,
    clock: Option[Clock]
  ): Pdn =
    annotateStatus(winner, status) {
      annotateOpening(opening) {
        annotateTurns(p, analysis ?? (_.advices))
      }.copy(
        tags = p.tags + Tag(_.Annotator, netDomain)
      )
    }

  private def annotateStatus(winner: Option[Color], status: Status)(p: Pdn) =
    lidraughts.game.StatusText(status, winner, draughts.variant.Standard) match {
      case "" => p
      case text => p.updateLastPly(_.copy(result = text.some))
    }

  private def annotateOpening(opening: Option[FullOpening.AtPly])(p: Pdn) = opening.fold(p) { o =>
    p.updatePly(o.ply, _.copy(opening = o.opening.ecoName.some))
  }

  private def annotateTurns(p: Pdn, advices: List[Advice]): Pdn =
    advices.foldLeft(p) {
      case (pdn, advice) => pdn.updateTurn(advice.turn, turn =>
        turn.update(advice.color, move =>
          move.copy(
            glyphs = Glyphs.fromList(advice.judgment.glyph :: Nil),
            comments = List(advice.makeComment(true, true)),
            variations = makeVariation(turn, advice) :: Nil
          )))
    }

  private def makeVariation(turn: Turn, advice: Advice): List[Turn] =
    Turn.fromMoves(
      advice.info.variation take 20 map { san => Move(san) },
      turn plyOf advice.color
    )
}
