package lila.round

import chess.format.Fen
import chess.format.Fen
import chess.variant.Variant
import play.api.libs.json.*

import lila.socket.Step

object StepBuilder:

  private val logger = lila.round.logger.branch("StepBuilder")

  def apply(
      id: GameId,
      pgnMoves: Vector[String],
      variant: Variant,
      initialFen: Fen
  ): JsArray =
    chess.Replay.gameMoveWhileValid(pgnMoves, initialFen, variant) match
      case (init, games, error) =>
        error foreach logChessError(id.value)
        JsArray {
          val initStep = Step(
            ply = init.turns,
            move = none,
            fen = Fen write init,
            check = init.situation.check,
            dests = None,
            drops = None,
            crazyData = init.situation.board.crazyData
          )
          val moveSteps = games.map { case (g, m) =>
            Step(
              ply = g.turns,
              move = Step.Move(m.uci, m.san).some,
              fen = Fen write g,
              check = g.situation.check,
              dests = None,
              drops = None,
              crazyData = g.situation.board.crazyData
            )
          }
          (initStep :: moveSteps).map(_.toJson)
        }

  private val logChessError = (id: String) =>
    (err: String) => {
      val path = if (id == "synthetic") "analysis" else id
      logger.info(s"https://lichess.org/$path ${err.linesIterator.toList.headOption | "?"}")
    }
