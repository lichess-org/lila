package lila.tutor

import lila.game.Game
import lila.analyse.Analysis
import lila.user.User
import lila.game.Pov
import lila.game.ClockHistory
import chess.Clock
import chess.Division

case class TutorTimeReport(
    games: NbGames,
    moves: NbMoves,
    timePressure: NbGames, // low time with less time than opponent
    defeatWithHighTime: NbGames,
    // defeatByFlagWithGoodPosition: NbGames,
    slowOpening: NbGames,
    immediateNonForcedMoves: NbMovesRatio // excepted in time pressure
) {}

object TutorTimeReport {

  val empty = TutorTimeReport(
    games = NbGames(0),
    moves = NbMoves(0),
    timePressure = NbGames(0),
    defeatWithHighTime = NbGames(0),
    // defeatByFlagWithGoodPosition = NbGames(0),
    slowOpening = NbGames(0),
    immediateNonForcedMoves = NbMovesRatio(0, 0)
  )

  def aggregate(time: TutorTimeReport, richPov: RichPov) = {
    val pov = richPov.pov
    for {
      clock   <- pov.game.clock
      history <- pov.game.clockHistory
      config = clock.config
    } yield {

      val isDefeatWithHighTime =
        ~pov.loss && history.last(pov.color).exists { finalTime =>
          finalTime.centis > clock.estimateTotalTime.centis / 3
        }

      val isTimePressure =
        clock.estimateTotalTime / 10 exists { pressurePoint =>
          fixIndex(history(pov.color).indexWhere(_ < pressurePoint)).exists { index =>
            index < history(pov.color).size - 5 &&                    // more than 5 moves were played after time pressure
            history(!pov.color).lift(index).exists(_ > pressurePoint) // the opponent had more time
          }
        }

      val isSlowOpening = history(pov.color).lift(5).exists {
        _.centis < clock.estimateTotalTime.centis * 8 / 10
      }

      val countImmediateNonForcedMoves: NbMovesRatio = NbMovesRatio(0, pov.moves)

      TutorTimeReport(
        games = time.games + 1,
        moves = time.moves + pov.moves,
        timePressure = time.timePressure inc isTimePressure,
        defeatWithHighTime = time.defeatWithHighTime inc isDefeatWithHighTime,
        slowOpening = time.slowOpening inc isSlowOpening,
        immediateNonForcedMoves = time.immediateNonForcedMoves + countImmediateNonForcedMoves
      )
    }
  } | time

  private def fixIndex(index: Int): Option[Int] = (index > -1) option index
}
