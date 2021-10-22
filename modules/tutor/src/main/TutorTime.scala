package lila.tutor

import lila.game.Game
import lila.analyse.Analysis
import lila.user.User
import lila.game.Pov
import lila.game.ClockHistory
import chess.Clock

case class TutorTimeReport(
    games: NbGames,
    moves: NbMoves,
    // timePressure: NbGames, // low time with less time than opponent
    defeatWithHighTime: NbGames
    // defeatByFlagWithGoodPosition: NbGames,
    // enterMidgameWithLowTime: NbGames,
    // immediateNonForcedMoves: NbMovesRatio // excepted in time pressure
) {}

object TutorTimeReport {

  val empty = TutorTimeReport(
    games = NbGames(0),
    moves = NbMoves(0),
    // timePressure = NbGames(0),
    defeatWithHighTime = NbGames(0)
    // defeatByFlagWithGoodPosition = NbGames(0),
    // enterMidgameWithLowTime = NbGames(0),
    // immediateNonForcedMoves = NbMovesRatio(0, 0)
  )

  def aggregate(time: TutorTimeReport, pov: Pov, analysis: Option[Analysis]) = {
    for {
      clock        <- pov.game.clock
      clockHistory <- pov.game.clockHistory
    } yield TutorTimeReport(
      games = time.games + 1,
      moves = time.moves + pov.moves,
      defeatWithHighTime =
        time.defeatWithHighTime + isDefeatWithHighTime(pov, clock.config, clockHistory).??(1)
    )
  } | time

  private def isDefeatWithHighTime(pov: Pov, clock: Clock.Config, clockHistory: ClockHistory): Boolean =
    ~pov.loss && clockHistory.last(pov.color).exists { finalTime =>
      finalTime.centis > clock.estimateTotalTime.centis / 3
    }
}
