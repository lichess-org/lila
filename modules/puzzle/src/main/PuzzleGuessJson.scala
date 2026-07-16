package lila.puzzle

import play.api.libs.json.*

import lila.common.Json.given

final class PuzzleGuessJson:

  def pref(p: lila.core.pref.Pref): JsObject =
    Json.obj(
      "coords" -> p.coords,
      "rookCastle" -> p.rookCastle,
      "destination" -> p.destination,
      "moveEvent" -> p.moveEvent,
      "highlight" -> p.highlight,
      "is3d" -> p.is3d,
      "animation" -> p.animationMillisForSpeedPuzzles,
      "ratings" -> p.showRatings
    )

  def position(g: PuzzleGuess): JsObject =
    Json.obj(
      "id" -> g.id.value,
      "fen" -> g.fen,
      "color" -> g.color.name
    )

  def player(p: PuzzleGuess.Player): JsObject =
    Json
      .obj(
        "rating" -> p.intRating,
        "runs" -> p.runs,
        "wins" -> p.wins
      )
      .add("provisional" -> p.glicko.provisional)

  def result(r: PuzzleGuessApi.Result): JsObject =
    Json
      .obj(
        "correct" -> r.correct,
        "isPuzzle" -> r.guess.isPuzzle,
        "finished" -> r.finished
      )
      .add("win" -> r.win.map(_.yes))
      .add("solution" -> r.solution.map(_.map(_.uci)))
      .add("positionRating" -> r.finished.option(r.guess.glicko.intRating))
      .add("ratingDiff" -> r.rating.map: (before, after) =>
        Json.obj("before" -> before, "after" -> after))
