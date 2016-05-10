package lila.analyse

import chess.format.pgn.{ Pgn, Tag, Turn, Move, Glyphs }
import chess.opening._
import chess.{ Status, Color, Clock }

private[analyse] final class Annotator(netDomain: String) {

  def apply(
    p: Pgn,
    analysis: Option[Analysis],
    opening: Option[FullOpening.AtPly],
    winner: Option[Color],
    status: Status,
    clock: Option[Clock]): Pgn =
    annotateStatus(winner, status) {
      annotateOpening(opening) {
        annotateTurns(p, analysis ?? (_.advices))
      }.copy(
        tags = p.tags :+ Tag("Annotator", netDomain)
      )
    }

  import chess.{ Status => S }
  private def annotateStatus(winner: Option[Color], status: Status)(p: Pgn) = (winner match {
    case Some(color) =>
      val loserName = (!color).toString.capitalize
      status match {
        case Status.Mate      => s"$loserName is checkmated".some
        case Status.Resign    => s"$loserName resigns".some
        case Status.Timeout   => s"$loserName leaves the game".some
        case Status.Outoftime => s"$loserName forfeits on time".some
        case Status.Cheat     => s"$loserName forfeits by computer assistance".some
        case _                => none
      }
    case None => status match {
      case Status.Aborted   => "Game is aborted".some
      case Status.Stalemate => "Stalemate".some
      case Status.Draw      => "Draw".some
      case _                => none
    }
  }) match {
    case Some(text) => p.updateLastPly(_.copy(result = text.some))
    case None       => p
  }

  private def annotateOpening(opening: Option[FullOpening.AtPly])(p: Pgn) = opening.fold(p) { o =>
    p.updatePly(o.ply, _.copy(opening = o.opening.ecoName.some))
  }

  private def annotateTurns(p: Pgn, advices: List[Advice]): Pgn =
    advices.foldLeft(p) {
      case (pgn, advice) => pgn.updateTurn(advice.turn, turn =>
        turn.update(advice.color, move =>
          move.copy(
            glyphs = Glyphs.fromList(advice.judgment.glyph :: Nil),
            comments = List(advice.makeComment(true, true)),
            variations = makeVariation(turn, advice) :: Nil
          )
        )
      )
    }

  private def makeVariation(turn: Turn, advice: Advice): List[Turn] =
    Turn.fromMoves(
      advice.info.variation take 20 map { san => Move(san) },
      turn plyOf advice.color
    )
}
