package lila.coach

import lila.socket.Step
import play.api.libs.json.JsArray

private object OpeningSteps {

  private val cache = scala.collection.mutable.Map[String, Option[JsArray]]()

  def apply(familyName: String): Option[JsArray] = cache.getOrElseUpdate(
    familyName,
    chess.Openings.familyMoveList get familyName map { moves =>
      lila.round.StepBuilder(
        id = familyName,
        pgnMoves = moves,
        variant = chess.variant.Standard,
        a = none,
        initialFen = chess.format.Forsyth.initial)
    }
  )
}
