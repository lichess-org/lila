package lila.round

import shogi.format.{ FEN, Forsyth }
import shogi.format.usi.Usi
import shogi.variant.Variant
import lila.socket.Step

import play.api.libs.json._

object StepBuilder {

  private val logger = lila.round.logger.branch("StepBuilder")

  def apply(
      id: String,
      usiMoves: Vector[Usi],
      variant: Variant,
      initialFen: Option[FEN]
  ): JsArray = {
    shogi.Replay.gamesWhileValid(usiMoves, initialFen, variant) match {
      case (games, error) =>
        error foreach logShogiError(id)
        val init = games.head
        JsArray {
          val initStep = Step(
            ply = init.turns,
            usi = none,
            fen = Forsyth >> init,
            check = init.situation.check,
            dests = None,
            drops = None
          )
          val moveSteps = games.tail.zip(usiMoves).map { case (g, u) =>
            Step(
              ply = g.turns,
              usi = u.some,
              fen = Forsyth >> g,
              check = g.situation.check,
              dests = None,
              drops = None
            )
          }
          (initStep :: moveSteps).map(_.toJson)
        }
    }
  }

  private val logShogiError = (id: String) =>
    (err: String) => {
      val path = if (id == "synthetic") "analysis" else id
      logger.info(s"https://lishogi.org/$path ${err.linesIterator.toList.headOption | "?"}")
    }
}
