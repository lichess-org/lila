package lila.puzzle

import play.api.i18n.Lang
import play.api.libs.json.*

import lila.common.Json.{ *, given }
import lila.common.paginator.{ Paginator, PaginatorJson }
import lila.game.GameRepo
import lila.rating.Perf
import lila.tree
import lila.tree.Node.defaultNodeJsonWriter
import lila.user.User

final class JsonView(
    gameJson: GameJson,
    gameRepo: GameRepo
)(using ec: scala.concurrent.ExecutionContext):

  import JsonView.{ *, given }

  def apply(
      puzzle: Puzzle,
      angle: Option[PuzzleAngle],
      replay: Option[PuzzleReplay],
      user: Option[User]
  )(using lang: Lang): Fu[JsObject] =
    gameJson(
      gameId = puzzle.gameId,
      plies = puzzle.initialPly,
      bc = false
    ) map { gameJson =>
      Json
        .obj(
          "game"   -> gameJson,
          "puzzle" -> puzzleJson(puzzle)
        )
        .add("user" -> user.map(userJson))
        .add("replay" -> replay.map(replayJson))
        .add(
          "angle",
          angle.map { a =>
            Json
              .obj(
                "key" -> a.key,
                "name" -> {
                  if (a == PuzzleAngle.mix) lila.i18n.I18nKeys.puzzle.puzzleThemes.txt()
                  else a.name.txt()
                },
                "desc" -> a.description.txt()
              )
              .add("chapter" -> a.asTheme.flatMap(PuzzleTheme.studyChapterIds.get))
              .add("opening" -> a.opening.map { op =>
                Json.obj("key" -> op.key, "name" -> op.name)
              })
          }
        )
    }

  def userJson(u: User) =
    Json
      .obj(
        "id"     -> u.id,
        "rating" -> u.perfs.puzzle.intRating
      )
      .add(
        "provisional" -> u.perfs.puzzle.provisional
      )

  private def replayJson(r: PuzzleReplay) =
    Json.obj("days" -> r.days, "i" -> r.i, "of" -> r.nb)

  def roundJson(u: User, round: PuzzleRound, perf: Perf) =
    Json
      .obj(
        "win"        -> round.win,
        "ratingDiff" -> (perf.intRating.value - u.perfs.puzzle.intRating.value)
      )
      .add("vote" -> round.vote)
      .add("themes" -> round.nonEmptyThemes.map { rt =>
        JsObject(rt.map { t =>
          t.theme.value -> JsBoolean(t.vote)
        })
      })

  def pref(p: lila.pref.Pref) =
    Json.obj(
      "blindfold"    -> p.blindfold,
      "coords"       -> p.coords,
      "keyboardMove" -> p.keyboardMove,
      "rookCastle"   -> p.rookCastle,
      "animation"    -> Json.obj("duration" -> p.animationMillis),
      "destination"  -> p.destination,
      "moveEvent"    -> p.moveEvent,
      "highlight"    -> p.highlight,
      "is3d"         -> p.is3d
    )

  def dashboardJson(dash: PuzzleDashboard, days: Int)(using lang: Lang) = Json.obj(
    "days"   -> days,
    "global" -> dashboardResults(dash.global),
    "themes" -> JsObject(dash.byTheme.toList.sortBy(-_._2.nb).map { case (key, res) =>
      key.value -> Json.obj(
        "theme"   -> PuzzleTheme(key).name.txt(),
        "results" -> dashboardResults(res)
      )
    })
  )

  private def dashboardResults(res: PuzzleDashboard.Results) = Json.obj(
    "nb"              -> res.nb,
    "firstWins"       -> res.firstWins,
    "replayWins"      -> res.fixed,
    "puzzleRatingAvg" -> res.puzzleRatingAvg,
    "performance"     -> res.performance
  )

  object bc:

    def apply(puzzle: Puzzle, user: Option[User])(using
        lang: Lang
    ): Fu[JsObject] =
      gameJson(
        gameId = puzzle.gameId,
        plies = puzzle.initialPly,
        bc = true
      ) map { gameJson =>
        Json
          .obj(
            "game"   -> gameJson,
            "puzzle" -> puzzleJson(puzzle)
          )
          .add("user" -> user.map(_.perfs.puzzle.intRating).map(userJson))
      }

    def batch(puzzles: Seq[Puzzle], user: Option[User])(using
        lang: Lang
    ): Fu[JsObject] = for {
      games <- gameRepo.gameOptionsFromSecondary(puzzles.map(_.gameId))
      jsons <- (puzzles zip games).collect { case (puzzle, Some(game)) =>
        gameJson.noCacheBc(game, puzzle.initialPly) map { gameJson =>
          Json.obj(
            "game"   -> gameJson,
            "puzzle" -> puzzleJson(puzzle)
          )
        }
      }.sequenceFu
    } yield Json
      .obj("puzzles" -> jsons)
      .add("user" -> user.map(_.perfs.puzzle.intRating).map(userJson))

    def userJson(rating: IntRating) = Json.obj(
      "rating" -> rating,
      "recent" -> Json.arr()
    )

    private def puzzleJson(puzzle: Puzzle) = Json.obj(
      "id"         -> Puzzle.numericalId(puzzle.id),
      "realId"     -> puzzle.id,
      "rating"     -> puzzle.glicko.intRating,
      "attempts"   -> puzzle.plays,
      "fen"        -> puzzle.fen,
      "color"      -> puzzle.color.name,
      "initialPly" -> (puzzle.initialPly + 1),
      "gameId"     -> puzzle.gameId,
      "lines" -> puzzle.line.tail.reverse.foldLeft[JsValue](JsString("win")) { case (acc, move) =>
        Json.obj(move.uci -> acc)
      },
      "vote"   -> 0,
      "branch" -> makeBranch(puzzle).map(defaultNodeJsonWriter.writes)
    )

    private def makeBranch(puzzle: Puzzle): Option[tree.Branch] =
      import chess.format.*
      val init = chess.Game(none, puzzle.fenAfterInitialMove.some).withTurns(puzzle.initialPly + 1)
      val (_, branchList) = puzzle.line.tail.foldLeft[(chess.Game, List[tree.Branch])]((init, Nil)) {
        case ((prev, branches), uci) =>
          val (game, move) =
            prev(uci.orig, uci.dest, uci.promotion)
              .fold(err => sys error s"puzzle ${puzzle.id} $err", identity)
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

object JsonView:

  given OWrites[PuzzleRound.Theme] = Json.writes
  given OWrites[PuzzleRound.Id]    = Json.writes
  given OWrites[PuzzleRound]       = Json.writes

  private def puzzleJson(puzzle: Puzzle): JsObject = Json.obj(
    "id"         -> puzzle.id,
    "rating"     -> puzzle.glicko.intRating,
    "plays"      -> puzzle.plays,
    "initialPly" -> puzzle.initialPly,
    "solution"   -> puzzle.line.tail.map(_.uci),
    "themes"     -> simplifyThemes(puzzle.themes)
  )

  private def simplifyThemes(themes: Set[PuzzleTheme.Key]) =
    themes.filterNot(_ == PuzzleTheme.mate.key)

  given OWrites[PuzzleHistory.SessionRound] = OWrites { sessionRound =>
    Json.obj(
      "round"  -> sessionRound.round,
      "puzzle" -> puzzleJson(sessionRound.puzzle),
      "theme"  -> sessionRound.theme
    )
  }
  given OWrites[PuzzleHistory.PuzzleSession] = OWrites { puzzleSession =>
    Json.obj(
      "theme"   -> puzzleSession.theme,
      "puzzles" -> puzzleSession.puzzles.toList
    )
  }
