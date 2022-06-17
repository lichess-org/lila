package lila.tutor

import chess.Color
import chess.format.{ FEN, Forsyth }
import chess.opening.{ FullOpening, FullOpeningDB }

import lila.common.LilaOpeningFamily
import lila.game.Game

case class TutorOpenings(
    colors: Color.Map[TutorColorOpenings]
)

case class TutorColorOpenings(
    families: List[TutorOpeningFamily]
)

case class TutorOpeningFamily(
    family: LilaOpeningFamily,
    games: TutorMetric[Float],
    ratingGain: TutorMetric[Float],
    acpl: TutorMetric[Float]
)

object TutorOpeningReport {

  // type OpeningMap    = Color.Map[ColorOpenings]
  // type ColorOpenings = List[TutorOpeningReport]
}
