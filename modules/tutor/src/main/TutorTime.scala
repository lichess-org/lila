// package lila.tutor

// import lila.game.Game
// import lila.analyse.Analysis
//
//
// import lila.game.ClockHistory
// import chess.Clock
// import chess.Division
// import chess.Centis
// import chess.Status

// case class TutorTimeReport(
//     games: NbGames,
//     moves: NbMoves,
//     timePressure: NbGames, // low time with less time than opponent
//     defeatWithHighTime: NbGames,
//     defeatByFlagWithGoodPosition: NbGames,
//     slowOpening: NbGames,
//     immediateNonForcedMoves: NbMovesRatio // excepted in time pressure
// )

// object TutorTimeReport {

//   val empty = TutorTimeReport(
//     games = NbGames(0),
//     moves = NbMoves(0),
//     timePressure = NbGames(0),
//     defeatWithHighTime = NbGames(0),
//     defeatByFlagWithGoodPosition = NbGames(0),
//     slowOpening = NbGames(0),
//     immediateNonForcedMoves = NbMovesRatio(0, 0)
//   )

//   def aggregate(report: TutorTimeReport, richPov: RichPov) = {
//     import richPov._
//     for {
//       clock   <- pov.game.clock
//       history <- pov.game.clockHistory
//       config = clock.config
//     } yield {
//       val isDefeatWithHighTime =
//         ~pov.loss && history.last(pov.color).exists { finalTime =>
//           finalTime.centis > clock.estimateTotalTime.centis / 3
//         }
//       val isTimePressure =
//         clock.estimateTotalTime / 10 exists { pressurePoint =>
//           fixIndex(history(pov.color).indexWhere(_ < pressurePoint)).exists { index =>
//             index < history(pov.color).size - 5 &&                    // more than 5 moves were played after time pressure
//             history(!pov.color).lift(index).exists(_ > pressurePoint) // the opponent had more time
//           }
//         }
//       val isSlowOpening = history(pov.color).lift(5).exists {
//         _.centis < clock.estimateTotalTime.centis * 8 / 10
//       }
//       val countImmediateNonForcedMoves: NbMovesRatio =
//         if (config.estimateTotalSeconds < 300) NbMovesRatio(0, 0) // ignore everything faster than 5+0
//         else {
//           val minTimeToUse =
//             (config.estimateTotalTime / 200) getOrElse Centis(200) atLeast Centis(200) atMost Centis(600)
//           val timePressure = (config.estimateTotalTime / 4).get
//           val moves = (~pov.game.moveTimes(pov.color))
//             .zip(history(pov.color))
//             .zipWithIndex
//             .drop(3) // ignore first moves
//             .collect {
//               case ((timeUsed, timeLeft), index) if timeUsed < minTimeToUse && timeLeft > timePressure =>
//                 val situation = replay.lift(index * 2 + pov.color.fold(0, 1))
//                 situation.exists(_.moves.sizeIs > 1)
//             }

//           NbMovesRatio(moves.count(identity), moves.size)
//         }
//       val isDefeatByFlagWithGoodPosition =
//         (~pov.loss && pov.game.status == Status.Outoftime) so {
//           analysis match {
//             case Some(an) =>
//               an.infos.lastOption
//                 .flatMap(_.forceCentipawns)
//                 .map(_ * pov.color.fold(1, -1))
//                 .exists(_ > 300)
//             case None =>
//               (pov.game.board.materialImbalance * pov.color.fold(1, -1)) > 3
//           }
//         }

//       TutorTimeReport(
//         games = report.games + 1,
//         moves = report.moves + pov.moves,
//         timePressure = report.timePressure inc isTimePressure,
//         defeatWithHighTime = report.defeatWithHighTime inc isDefeatWithHighTime,
//         defeatByFlagWithGoodPosition = report.defeatByFlagWithGoodPosition inc isDefeatByFlagWithGoodPosition,
//         slowOpening = report.slowOpening inc isSlowOpening,
//         immediateNonForcedMoves = report.immediateNonForcedMoves + countImmediateNonForcedMoves
//       )
//     }
//   } | report

//   private def fixIndex(index: Int): Option[Int] = (index > -1) option index
// }
