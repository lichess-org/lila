package lila.puzzle

import play.api.libs.json._

object Export {

  def apply(puzzles: List[Puzzle]) = Json stringify {
    JsArray(
      puzzles map { puzzle =>
        JsString(Json stringify {
          Json.obj(
            "id" -> puzzle.id,
            "fen" -> puzzle.fen,
            "color" -> puzzle.color.name,
            "move" -> puzzle.initialMove,
            "ply" -> puzzle.initialPly,
            "game" -> puzzle.gameId,
            "lines" -> lila.puzzle.Line.toJson(puzzle.lines))
        })
      })
  }
}
