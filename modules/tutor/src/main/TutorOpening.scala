package lila.tutor

import lila.game.Game
import chess.Color
import chess.opening.{ FullOpening, FullOpeningDB }
import chess.format.{ FEN, Forsyth }

case class TutorOpeningReport(opening: FullOpening, games: NbGames, moves: NbMoves)

object TutorOpeningReport {

  type OpeningMap    = Color.Map[ColorOpenings]
  type ColorOpenings = List[TutorOpeningReport]
}
