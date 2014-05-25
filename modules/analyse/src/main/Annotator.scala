package lila.analyse

import chess.format.pgn.{ Pgn, Tag, Turn, Move }
import chess.OpeningExplorer.Opening
import chess.{ Status, Color, Clock }

private[analyse] final class Annotator(netDomain: String) {

  def apply(
    p: Pgn,
    analysis: Option[Analysis],
    opening: Option[Opening],
    winner: Option[Color],
    status: Status,
    clock: Option[Clock]): Pgn =
    annotateStatus(winner, status) {
      annotateOpening(opening) {
        annotateTurns(p, analysis ?? (_.advices))
      }.copy(
        tags = (p.tags ::: (clock ?? clockTags)) :+ Tag("Annotator", netDomain)
      )
    }

  private def clockTags(clock: Clock) = List(
    Tag(_.WhiteClock, clock showTime (clock remainingTime Color.White)),
    Tag(_.BlackClock, clock showTime (clock remainingTime Color.Black)))

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
