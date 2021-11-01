package lila.tutor

import lila.game.Game
import chess.Color

case class TutorOpeningReport(byColor: Color.Map[TutorOpeningColorReport])

case class TutorOpeningColorReport(games: NbGames, moves: NbMoves)

object TutorOpeningReport {

  val empty = TutorOpeningReport(Color.Map(TutorOpeningColorReport.empty, TutorOpeningColorReport.empty))

  def aggregate(report: TutorOpeningReport, richPov: RichPov) =
    TutorOpeningReport(report.byColor.update(richPov.pov.color, TutorOpeningColorReport.aggregate(richPov)))
}

object TutorOpeningColorReport {

  val empty = TutorOpeningColorReport(
    games = NbGames(0),
    moves = NbMoves(0)
  )
  def aggregate(richPov: RichPov)(report: TutorOpeningColorReport) = {
    import richPov._
    TutorOpeningColorReport(
      games = report.games + 1,
      moves = report.moves + division.openingSize / 2
    )
  }
}
