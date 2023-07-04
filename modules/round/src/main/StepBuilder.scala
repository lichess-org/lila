package lila.round

import chess.format.Fen
import chess.format.pgn.SanStr
import chess.variant.Variant
import play.api.libs.json.*

import lila.socket.Step

object StepBuilder:

  private val logger = lila.round.logger.branch("StepBuilder")

  def apply(
      id: GameId,
      sans: Vector[SanStr],
      variant: Variant,
      initialFen: Fen.Epd
  ): JsArray =
    chess.Replay.gameMoveWhileValid(sans, initialFen, variant) match
      case (init, games, error) =>
        error foreach logChessError(id.value)
        JsArray {
          val initStep = Step(
            ply = init.ply,
            move = none,
            fen = Fen write init,
            check = init.situation.check,
            dests = None,
            drops = None,
            crazyData = init.situation.board.crazyData
          )
          val moveSteps = games.map { case (g, m) =>
            Step(
              ply = g.ply,
              move = m.some,
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
    (err: chess.ErrorStr) =>
      val path = if id == "synthetic" then "analysis" else id
      logger.info(s"https://lichess.org/$path ${err.value.linesIterator.toList.headOption | "?"}")
