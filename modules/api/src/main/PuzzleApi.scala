package lila.api

import play.api.libs.json._

import lila.hub.actorApi.{ router => R }
import lila.puzzle.{ Line, Puzzle, DailyPuzzle }

private[api] final class PuzzleApi(env: lila.puzzle.Env, makeUrl: Any => Fu[String]) {

  def daily: Fu[Option[JsObject]] = env.daily() flatMap {
    case Some(p) => one(p.id)
    case None    => fuccess(none)
  }

  def one(id: Int): Fu[Option[JsObject]] = env.api.puzzle find id flatMap {
    case None    => fuccess(none)
    case Some(p) => toJson(p) map (_.some)
  }

  private def toJson(p: Puzzle) = makeUrl(R Puzzle p.id) map { url =>
    Json.obj(
      "id" -> p.id,
      "url" -> url,
      "color" -> p.color.name,
      "position" -> p.fen,
      "solution" -> (p.initialMove :: Line.solution(p.lines)),
      "rating" -> p.perf.intRating)
  }
}
