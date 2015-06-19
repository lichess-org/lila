package lila.puzzle

import play.api.libs.json._

object Export {

  def apply(puzzles: List[Puzzle]) = Json stringify {
    JsArray(puzzles map { puzzle =>
      Json.obj(
        "id" -> puzzle.id,
        "rating" -> puzzle.perf.intRating,
        "attempts" -> puzzle.attempts,
        "fen" -> puzzle.fen,
        "color" -> puzzle.color.name,
        "initialMove" -> puzzle.initialMove,
        "initialPly" -> puzzle.initialPly,
        "gameId" -> puzzle.gameId,
        "lines" -> lila.puzzle.Line.toJson(puzzle.lines),
        "vote" -> puzzle.vote.sum)
    })
  }
}
