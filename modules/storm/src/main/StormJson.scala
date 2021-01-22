package lila.storm

import play.api.libs.json._

import lila.common.Json._

final class StormJson {

  import StormJson.puzzleWrites

  def apply(puzzles: List[StormPuzzle]): JsObject = Json.obj(
    "puzzles" -> puzzles
  )
}

object StormJson {

  import lila.puzzle.JsonView.puzzleIdWrites

  implicit val puzzleWrites: OWrites[StormPuzzle] = OWrites { p =>
    Json.obj(
      "id"   -> p.id,
      "fen"  -> p.fen.value,
      "line" -> p.line.toList.map(_.uci)
    )
  }
}
