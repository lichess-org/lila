package lila.round

import shogi.format.forsyth.Sfen
import shogi.format.usi.Usi
import shogi.variant.Variant
import lila.socket.Step

import play.api.libs.json._

object StepBuilder {

  private val logger = lila.round.logger.branch("StepBuilder")

  def apply(
      id: String,
      usis: Vector[Usi],
      variant: Variant,
      initialSfen: Option[Sfen]
  ): JsArray = {
    shogi.Replay.gamesWhileValid(usis, initialSfen, variant) match {
      case (games, error) =>
        error foreach logShogiError(id)
        val init = games.head
        JsArray {
          val initStep = Step(
            ply = init.plies,
            usi = none,
            sfen = init.toSfen,
            check = init.situation.check
          )
          val moveSteps = games.tail.zip(usis).map { case (g, u) =>
            Step(
              ply = g.plies,
              usi = u.some,
              sfen = g.toSfen,
              check = g.situation.check
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
