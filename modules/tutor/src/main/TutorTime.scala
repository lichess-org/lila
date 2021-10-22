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
    timePressure: NbGames, // low time with less time than opponent
    defeatWithHighTime: NbGames
    // defeatByFlagWithGoodPosition: NbGames,
    // enterMidgameWithLowTime: NbGames,
    // immediateNonForcedMoves: NbMovesRatio // excepted in time pressure
) {}

object TutorTimeReport {

  val empty = TutorTimeReport(
    games = NbGames(0),
    moves = NbMoves(0),
    timePressure = NbGames(0),
    defeatWithHighTime = NbGames(0)
    // defeatByFlagWithGoodPosition = NbGames(0),
    // enterMidgameWithLowTime = NbGames(0),
    // immediateNonForcedMoves = NbMovesRatio(0, 0)
  )

  def aggregate(time: TutorTimeReport, pov: Pov, analysis: Option[Analysis]) = {
    for {
      clock   <- pov.game.clock
      history <- pov.game.clockHistory
      config = clock.config
    } yield TutorTimeReport(
      games = time.games + 1,
      moves = time.moves + pov.moves,
      timePressure = time.timePressure inc isTimePressure(pov, config, history),
      defeatWithHighTime = time.defeatWithHighTime inc isDefeatWithHighTime(pov, config, history)
    )
  } | time

  private def isDefeatWithHighTime(pov: Pov, clock: Clock.Config, history: ClockHistory): Boolean =
    ~pov.loss && history.last(pov.color).exists { finalTime =>
      finalTime.centis > clock.estimateTotalTime.centis / 3
    }

  private def isTimePressure(pov: Pov, clock: Clock.Config, history: ClockHistory) =
    clock.estimateTotalTime / 10 exists { pressurePoint =>
      fixIndex(history(pov.color).indexWhere(_ < pressurePoint)).exists { index =>
        index < history(pov.color).size - 5 &&                    // more than 5 moves were played after time pressure
        history(!pov.color).lift(index).exists(_ > pressurePoint) // the opponent had more time
      }
    }

  private def fixIndex(index: Int): Option[Int] = (index > -1) option index
}
