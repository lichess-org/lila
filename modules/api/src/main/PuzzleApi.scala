package lila.api

import play.api.libs.json._

import lila.hub.actorApi.{ router => R }
import lila.puzzle.{ Line, Puzzle, DailyPuzzle }

private[api] final class PuzzleApi(env: lila.puzzle.Env, makeUrl: Any => Fu[String]) {

  def daily: Fu[Option[JsObject]] = env.daily() flatMap {
    case Some(p) => one(p.id)
    case None    => fuccess(none)
  }

  def one(id: Int): Fu[Option[JsObject]] = env.api.puzzle find id map2 toJson

  private def toJson(p: Puzzle) = Json.obj(
    "id" -> p.id,
    "position" -> p.fen,
    "solution" -> Line.solution(p.lines),
    "rating" -> p.perf.intRating)
}

