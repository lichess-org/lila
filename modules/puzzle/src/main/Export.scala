package lila.puzzle

import java.nio.charset.StandardCharsets
import java.util.Base64
import play.api.libs.json._

object Export {

  private def base64(str: String) = Base64.getEncoder.encodeToString(str getBytes StandardCharsets.UTF_8)

  def apply(api: PuzzleApi, nb: Int) =
    api.puzzle.export(nb * 2).map { puzzles =>
      puzzles.map { puzzle =>
        val encoded = base64(Json stringify {
          Json.obj(
            "id" -> puzzle.id,
            "fen" -> puzzle.fen,
            "color" -> puzzle.color.name,
            "move" -> puzzle.initialMove.uci,
            "lines" -> lila.puzzle.Line.toJson(puzzle.lines))
        })
        s""""$encoded"""" -> puzzle.vote.sum
      }.sortBy(_._1.size)
        .take(nb)
        .sortBy(_._2 -> scala.util.Random.nextInt)
        .map(_._1)
        .mkString(",\n")
    }
}
