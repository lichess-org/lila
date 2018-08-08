package lidraughts.round

import draughts.format.Forsyth
import draughts.variant.Variant
import lidraughts.socket.Step

import play.api.libs.json._

object StepBuilder {

  private val logger = lidraughts.round.logger.branch("StepBuilder")

  def apply(
    id: String,
    pdnmoves: Vector[String],
    variant: Variant,
    initialFen: String
  ): JsArray = {
    draughts.Replay.gameMoveWhileValid(pdnmoves, initialFen, variant) match {
      case (init, games, error) =>
        error foreach logChessError(id)
        JsArray {
          val initStep = Step(
            ply = init.turns,
            move = none,
            fen = Forsyth >> init,
            dests = None,
            captLen = init.situation.allMovesCaptureLength
          )
          val moveSteps = games.map {
            case (g, m) =>
              Step(
                ply = g.turns,
                move = Step.Move(m.uci, m.san).some,
                fen = Forsyth >> g,
                dests = None,
                captLen = g.situation.allMovesCaptureLength
              )
          }
          (initStep :: moveSteps).map(_.toJson)
        }
    }
  }

  private val logChessError = (id: String) => (err: String) => {
    val path = if (id == "synthetic") "analysis" else id
    logger.info(s"https://lidraughts.org/$path ${err.lines.toList.headOption | "?"}")
  }
}
