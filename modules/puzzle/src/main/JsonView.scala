package lila.puzzle

import play.api.libs.json._

import lila.common.Json._
import lila.game.GameRepo
import lila.tree
import lila.tree.Node.defaultNodeJsonWriter

final class JsonView(
    gameJson: GameJson,
    gameRepo: GameRepo,
    animationDuration: scala.concurrent.duration.Duration
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(
      puzzle: Puzzle,
      userInfos: Option[UserInfos],
      mode: String,
      mobileApi: Option[lila.common.ApiVersion],
      round: Option[Round] = None,
      voted: Option[Boolean]
  ): Fu[JsObject] = {
    val isOldMobile = mobileApi.exists(_.value < 3)
    val isMobile    = mobileApi.isDefined
    (!isOldMobile ?? gameJson(
      gameId = puzzle.gameId,
      plies = puzzle.initialPly,
      onlyLast = isMobile
    ) dmap some) map { gameJson =>
      Json
        .obj(
          "game"   -> gameJson,
          "puzzle" -> puzzleJson(puzzle, isOldMobile),
          "mode"   -> mode,
          "attempt" -> round.ifTrue(isOldMobile).map { r =>
            Json.obj(
              "userRatingDiff" -> r.ratingDiff,
              "win"            -> r.result.win,
              "seconds"        -> "a few" // lol we don't have the value anymore
            )
          },
          "voted" -> voted,
          "user"  -> userInfos.map(JsonView.infos(isOldMobile)),
          "difficulty" -> isOldMobile.option {
            Json.obj(
              "choices" -> Json.arr(
                Json.arr(2, "Normal")
              ),
              "current" -> 2
            )
          }
        )
        .noNull
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

  def batch(puzzles: List[Puzzle], userInfos: UserInfos): Fu[JsObject] =
    for {
      games <- gameRepo.gameOptionsFromSecondary(puzzles.map(_.gameId))
      jsons <- (puzzles zip games).collect { case (puzzle, Some(game)) =>
        gameJson.noCache(game, puzzle.initialPly, onlyLast = true) map { gameJson =>
          Json.obj(
            "game"   -> gameJson,
            "puzzle" -> puzzleJson(puzzle, isOldMobile = false)
          )
        }
      }.sequenceFu
    } yield Json.obj(
      "user"    -> JsonView.infos(isOldMobile = false)(userInfos),
      "puzzles" -> jsons
    )

  private def puzzleJson(puzzle: Puzzle, isOldMobile: Boolean): JsObject =
    Json
      .obj(
        "id"         -> puzzle.id,
        "rating"     -> puzzle.perf.intRating,
        "attempts"   -> puzzle.attempts,
        "fen"        -> puzzle.fen,
        "color"      -> puzzle.color.name,
        "initialPly" -> puzzle.initialPly,
        "gameId"     -> puzzle.gameId,
        "lines"      -> lila.puzzle.Line.toJson(puzzle.lines),
        "vote"       -> puzzle.vote.sum
      )
      .add("initialMove" -> isOldMobile.option(puzzle.initialMove.uci))
      .add("branch" -> (!isOldMobile).??(makeBranch(puzzle)).map(defaultNodeJsonWriter.writes))
      .add("enabled" -> puzzle.enabled)

  private def makeBranch(puzzle: Puzzle): Option[tree.Branch] = {
    import chess.format._
    val fullSolution: List[Uci.Move] = (Line solution puzzle.lines).map { uci =>
      Uci.Move(uci) err s"Invalid puzzle solution UCI $uci"
    }
    val solution =
      if (fullSolution.isEmpty) {
        logger.warn(s"Puzzle ${puzzle.id} has an empty solution from ${puzzle.lines}")
        fullSolution
      } else if (fullSolution.size % 2 == 0) fullSolution.init
      else fullSolution
    val init = chess.Game(none, puzzle.fenAfterInitialMove).withTurns(puzzle.initialPly)
    val (_, branchList) = solution.foldLeft[(chess.Game, List[tree.Branch])]((init, Nil)) {
      case ((prev, branches), uci) =>
        val (game, move) =
          prev(uci.orig, uci.dest, uci.promotion).fold(err => sys error s"puzzle ${puzzle.id} $err", identity)
        val branch = tree.Branch(
          id = UciCharPair(move.toUci),
          ply = game.turns,
          move = Uci.WithSan(move.toUci, game.pgnMoves.last),
          fen = chess.format.Forsyth >> game,
          check = game.situation.check,
          crazyData = none
        )
        (game, branch :: branches)
    }
    branchList.foldLeft[Option[tree.Branch]](None) {
      case (None, branch)        => branch.some
      case (Some(child), branch) => Some(branch addChild child)
    }
  }
}

object JsonView {

  def infos(isOldMobile: Boolean)(i: UserInfos): JsObject =
    Json
      .obj(
        "rating"  -> i.user.perfs.puzzle.intRating,
        "history" -> isOldMobile.option(i.history.map(_.rating)), // for mobile BC
        "recent" -> i.history.map { r =>
          Json.arr(r.id.puzzleId, r.ratingDiff, r.rating)
        }
      )
      .noNull

  def round(r: Round): JsObject =
    Json.obj(
      "ratingDiff" -> r.ratingDiff,
      "win"        -> r.result.win
    )
}
