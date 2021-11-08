package lila.tutor

import lila.game.Game
import chess.Color
import chess.opening.{ FullOpening, FullOpeningDB }
import chess.format.{ FEN, Forsyth }

case class TutorOpeningReport(opening: FullOpening, games: NbGames, moves: NbMoves)

object TutorOpeningReport {

  type OpeningMap      = Color.Map[ColorOpeningMap]
  type ColorOpeningMap = Map[FEN, TutorOpeningReport]

  def aggregate(openings: OpeningMap, richPov: RichPov) = {

    import richPov._

    val opening = pov.game.variant.standard ??
      replay
        .map(s => FEN(Forsyth exportStandardPositionTurnCastlingEp s))
        .zipWithIndex
        .drop(1)
        .foldRight(none[FullOpening.AtPly]) {
          case ((fen, ply), None) => FullOpeningDB.findByFen(fen).map(_ atPly ply)
          case (_, found)         => found
        }

    opening.fold(openings) { op =>
      openings.update(
        pov.color,
        _.updatedWith(FEN(op.opening.fen)) { opt =>
          val prev = opt | TutorOpeningReport(op.opening, NbGames(0), NbMoves(0))
          prev
            .copy(
              games = prev.games + 1,
              moves = prev.moves + op.ply / 2
            )
            .some
        }
      )
    }
  }
}
