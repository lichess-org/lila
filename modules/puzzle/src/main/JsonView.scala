package lila.puzzle

import play.api.libs.json._

import lila.common.Json._
import lila.game.GameRepo
import lila.tree
import lila.tree.Node.defaultNodeJsonWriter
import lila.user.User

final class JsonView(
    gameJson: GameJson,
    gameRepo: GameRepo,
    animationDuration: scala.concurrent.duration.Duration
)(implicit ec: scala.concurrent.ExecutionContext) {

  import JsonView._

  def apply(
      puzzle: Puzzle,
      user: Option[User],
      mobileApi: Option[lila.common.ApiVersion],
      round: Option[Round] = None
  ): Fu[JsObject] = {
    gameJson(
      gameId = puzzle.gameId,
      plies = puzzle.initialPly,
      onlyLast = mobileApi.isDefined
    ) map { gameJson =>
      Json
        .obj(
          "game"   -> gameJson,
          "puzzle" -> puzzleJson(puzzle)
        )
        .add("user" -> user.map { u =>
          Json.obj("rating" -> u.perfs.puzzle.intRating)
        })
    }
  }

  def pref(p: lila.pref.Pref) =
    Json.obj(
      "blindfold"  -> p.blindfold,
      "coords"     -> p.coords,
      "rookCastle" -> p.rookCastle,
      "animation" -> Json.obj(
        "duration" -> p.animationFactor * animationDuration.toMillis
      ),
      "destination"  -> p.destination,
      "resizeHandle" -> p.resizeHandle,
      "moveEvent"    -> p.moveEvent,
      "highlight"    -> p.highlight,
      "is3d"         -> p.is3d
    )

  private def puzzleJson(puzzle: Puzzle): JsObject =
    Json
      .obj(
        "id"         -> puzzle.id,
        "rating"     -> puzzle.glicko.intRating,
        "plays"      -> puzzle.plays,
        "fen"        -> puzzle.fen,
        "color"      -> puzzle.color.name,
        "initialPly" -> puzzle.initialPly,
        "gameId"     -> puzzle.gameId,
        "line"       -> puzzle.line.toList.map(_.uci).mkString(" "),
        "vote"       -> puzzle.vote
      )
}

object JsonView {

  implicit val puzzleIdWrites: Writes[Puzzle.Id] = stringIsoWriter(Puzzle.idIso)
}
