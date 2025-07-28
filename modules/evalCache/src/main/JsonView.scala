package lila.evalCache

import chess.format.Fen
import play.api.libs.json.*

import lila.common.Json.given
import lila.tree.{ CloudEval, Pv }

object JsonView:

  def writeEval(e: CloudEval, fen: Fen.Full) =
    Json.obj(
      "fen" -> fen,
      "knodes" -> e.knodes,
      "depth" -> e.depth,
      "pvs" -> JsArray(e.pvs.toList.map(writePv))
    )

  private def writePv(pv: Pv) = Json
    .obj("moves" -> pv.moves.value.toList.map(_.uci).mkString(" "))
    .add("cp", pv.score.cp)
    .add("mate", pv.score.mate)
