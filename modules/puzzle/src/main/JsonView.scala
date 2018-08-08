package lidraughts.puzzle

import draughts.format.{ Uci, UciCharPair }
import play.api.libs.json._
import lidraughts.game.GameRepo
import lidraughts.tree

final class JsonView(
    gameJson: GameJson,
    animationDuration: scala.concurrent.duration.Duration
) {

  def apply(
    puzzle: Puzzle,
    userInfos: Option[UserInfos],
    mode: String,
    mobileApi: Option[lidraughts.common.ApiVersion],
    round: Option[Round] = None,
    result: Option[Result] = None,
    voted: Option[Boolean]
  ): Fu[JsObject] = {
    val isOldMobile = mobileApi.exists(_.value < 3)
    val isMobile = mobileApi.isDefined
    ((!isOldMobile && puzzle.gameId != "custom") ?? gameJson(
      gameId = puzzle.gameId,
      plies = puzzle.initialPly,
      onlyLast = isMobile
    ) map some) map { gameJson =>
      Json.obj(
        "game" -> gameJson,
        "puzzle" -> puzzleJson(puzzle, isOldMobile),
        "history" -> tree.Branch(
          id = UciCharPair(puzzle.initialMove),
          ply = puzzle.initialPly,
          move = Uci.WithSan(puzzle.initialMove, puzzle.initialMove.toSan),
          fen = puzzle.fenAfterInitialMove.getOrElse(puzzle.fen)
        ),
        "mode" -> mode,
        "attempt" -> round.ifTrue(isOldMobile).map { r =>
          Json.obj(
            "userRatingDiff" -> r.ratingDiff,
            "win" -> r.result.win,
            "seconds" -> "a few" // lol we don't have the value anymore
          )
        },
        "voted" -> voted,
        "user" -> userInfos.map(JsonView.infos(isOldMobile)),
        "difficulty" -> isOldMobile.option {
          Json.obj(
            "choices" -> Json.arr(
              Json.arr(2, "Normal")
            ),
            "current" -> 2
          )
        }
      ).noNull
    }
  }

  def pref(p: lidraughts.pref.Pref) = Json.obj(
    "coords" -> p.coords,
    "animation" -> Json.obj(
      "duration" -> p.animationFactor * animationDuration.toMillis
    ),
    "destination" -> p.destination,
    "moveEvent" -> p.moveEvent,
    "highlight" -> p.highlight
  )

  def batch(puzzles: List[Puzzle], userInfos: UserInfos): Fu[JsObject] = for {
    games <- GameRepo.gameOptionsFromSecondary(puzzles.map(_.gameId))
    jsons <- (puzzles zip games).collect {
      case (puzzle, Some(game)) =>
        gameJson.noCache(game, puzzle.initialPly, true) map { gameJson =>
          Json.obj(
            "game" -> gameJson,
            "puzzle" -> puzzleJson(puzzle, isOldMobile = false)
          )
        }
    }.sequenceFu
  } yield Json.obj(
    "user" -> JsonView.infos(false)(userInfos),
    "puzzles" -> jsons
  )

  private def puzzleJson(puzzle: Puzzle, isOldMobile: Boolean): JsObject = Json.obj(
    "id" -> puzzle.id,
    "rating" -> puzzle.perf.intRating,
    "attempts" -> puzzle.attempts,
    "fen" -> puzzle.fen,
    "color" -> puzzle.color.name,
    "initialPly" -> puzzle.initialPly,
    "gameId" -> puzzle.gameId,
    "lines" -> lidraughts.puzzle.Line.toJson(puzzle.lines),
    "vote" -> puzzle.vote.sum
  ).add("initialMove" -> isOldMobile.option(puzzle.initialMove.uci))
    .add("branch" -> (!isOldMobile).option(makeBranch(puzzle)))
    .add("enabled" -> puzzle.enabled)

  private def makeBranch(puzzle: Puzzle): Option[tree.Branch] = {
    import draughts.format._
    val fullSolution: List[Uci.Move] = (Line solution puzzle.lines).map { uci =>
      Uci.Move(uci) err s"Invalid puzzle solution UCI $uci"
    }
    val solution =
      if (fullSolution.isEmpty) {
        logger.warn(s"Puzzle ${puzzle.id} has an empty solution from ${puzzle.lines}")
        fullSolution
      } //else if (fullSolution.size % 2 == 0) fullSolution.init
      else fullSolution
    val init = draughts.DraughtsGame(none, puzzle.fenAfterInitialMove).withTurns(puzzle.initialPly)
    val (_, branchList) = solution.foldLeft[(draughts.DraughtsGame, List[tree.Branch])]((init, Nil)) {
      case ((prev, branches), uci) =>
        val (game, move) = prev(uci.orig, uci.dest, uci.promotion).prefixFailuresWith(s"puzzle ${puzzle.id}").err
        val branch = tree.Branch(
          id = UciCharPair(move.toUci),
          ply = game.turns,
          move = Uci.WithSan(move.toShortUci, game.pdnMoves.last),
          fen = draughts.format.Forsyth >> game
        )
        (game, branch :: branches)
    }
    branchList.foldLeft[Option[tree.Branch]](None) {
      case (None, branch) => branch.some
      case (Some(child), branch) => Some(branch addChild child)
    }
  }
}

object JsonView {

  def infos(isOldMobile: Boolean)(i: UserInfos): JsObject = Json.obj(
    "rating" -> i.user.perfs.puzzle.intRating,
    "history" -> isOldMobile.option(i.history.map(_.rating)), // for mobile BC
    "recent" -> i.history.map { r =>
      Json.arr(r.puzzleId, r.ratingDiff, r.rating)
    }
  ).noNull

  def round(r: Round): JsObject = Json.obj(
    "ratingDiff" -> r.ratingDiff,
    "win" -> r.result.win
  )
}
