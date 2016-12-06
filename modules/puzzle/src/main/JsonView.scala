package lila.puzzle

import play.api.libs.json._

import lila.common.PimpedJson._
import lila.game.Game
import lila.puzzle._
import lila.tree

object JsonView {

  def apply(
    puzzle: Puzzle,
    userInfos: Option[UserInfos],
    mode: String,
    animationDuration: scala.concurrent.duration.Duration,
    pref: lila.pref.Pref,
    isMobileApi: Boolean,
    round: Option[Round] = None,
    win: Option[Boolean] = None,
    voted: Option[Boolean]): Fu[JsObject] =
    (!isMobileApi ?? GameJson(puzzle.gameId, puzzle.initialPly).map(_.some)) map { gameJson =>
      Json.obj(
        "game" -> gameJson,
        "puzzle" -> Json.obj(
          "id" -> puzzle.id,
          "rating" -> puzzle.perf.intRating,
          "attempts" -> puzzle.attempts,
          "fen" -> puzzle.fen,
          "color" -> puzzle.color.name,
          "initialMove" -> puzzle.initialMove.uci,
          "initialPly" -> puzzle.initialPly,
          "gameId" -> puzzle.gameId,
          "lines" -> lila.puzzle.Line.toJson(puzzle.lines),
          "branch" -> makeBranch(puzzle),
          "enabled" -> puzzle.enabled,
          "vote" -> puzzle.vote.sum
        ),
        "pref" -> Json.obj(
          "coords" -> pref.coords,
          "rookCastle" -> pref.rookCastle
        ),
        "chessground" -> (!isMobileApi).option(Json.obj(
          "highlight" -> Json.obj(
            "lastMove" -> pref.highlight,
            "check" -> pref.highlight
          ),
          "movable" -> Json.obj(
            "showDests" -> pref.destination
          ),
          "draggable" -> Json.obj(
            "showGhost" -> pref.highlight
          ),
          "premovable" -> Json.obj(
            "showDests" -> pref.destination
          )
        )),
        "animation" -> Json.obj(
          "duration" -> pref.animationFactor * animationDuration.toMillis
        ),
        "mode" -> mode,
        "round" -> round.map { a =>
          Json.obj(
            "ratingDiff" -> a.ratingDiff,
            "win" -> a.win
          )
        },
        "attempt" -> round.ifTrue(isMobileApi).map { r =>
          Json.obj(
            "userRatingDiff" -> r.ratingDiff,
            "win" -> r.win,
            "seconds" -> "a few" // lol we don't have the value anymore
          )
        },
        "win" -> win,
        "voted" -> voted,
        "user" -> userInfos.map { i =>
          Json.obj(
            "rating" -> i.user.perfs.puzzle.intRating,
            "history" -> isMobileApi.option(i.history.map(_.rating)), // for mobile BC
            "recent" -> i.history.map { r =>
              Json.arr(r.puzzleId, r.ratingDiff, r.rating)
            }
          ).noNull
        },
        "difficulty" -> isMobileApi.option {
          Json.obj(
            "choices" -> Json.arr(
              Json.arr(2, "Normal")
            ),
            "current" -> 2
          )
        }).noNull
    }

  private def makeBranch(puzzle: Puzzle): Option[tree.Branch] = {
    import chess.format._
    val solution: List[Uci.Move] = Line solution puzzle.lines map { uci =>
      Uci.Move(uci) err s"Invalid puzzle solution UCI $uci"
    }
    val init = chess.Game(none, puzzle.fenAfterInitialMove).withTurns(puzzle.initialPly)
    val (_, branchList) = solution.foldLeft[(chess.Game, List[tree.Branch])]((init, Nil)) {
      case ((prev, branches), uci) =>
        val (game, _) = prev(uci.orig, uci.dest, uci.promotion).prefixFailuresWith(s"puzzle ${puzzle.id}").err
        val branch = tree.Branch(
          id = UciCharPair(uci),
          ply = game.turns,
          move = Uci.WithSan(uci, game.pgnMoves.last),
          fen = chess.format.Forsyth >> game,
          check = game.situation.check,
          crazyData = none)
        (game, branch :: branches)
    }
    branchList.foldLeft[Option[tree.Branch]](None) {
      case (None, branch)        => branch.some
      case (Some(child), branch) => Some(branch addChild child)
    }
  }
}
