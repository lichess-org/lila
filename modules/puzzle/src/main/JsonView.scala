package lila.puzzle

import play.api.i18n.Lang
import play.api.libs.json._

import lila.common.Json._
import lila.game.GameRepo
import lila.rating.Perf
import lila.tree
import lila.tree.Node.defaultNodeJsonWriter
import lila.user.User

final class JsonView(
    gameJson: GameJson,
    gameRepo: GameRepo
)(implicit ec: scala.concurrent.ExecutionContext) {

  import JsonView._

  def apply(puzzle: Puzzle, theme: PuzzleTheme, replay: Option[PuzzleReplay], user: Option[User])(implicit
      lang: Lang
  ): Fu[JsObject] = {
    puzzle.gameId.fold(fuccess(otherSourcesJson(puzzle))) { gid =>
      gameJson(
        gameId = gid,
        plies = puzzle.initialPly,
        bc = false
      )
    } map { gameJson =>
      Json
        .obj(
          "game"   -> gameJson,
          "puzzle" -> puzzleJson(puzzle),
          "theme" -> Json
            .obj(
              "key" -> theme.key,
              "name" -> {
                if (theme == PuzzleTheme.mix) lila.i18n.I18nKeys.puzzle.puzzleThemes.txt()
                else theme.name.txt()
              },
              "desc" -> theme.description.txt()
            )
            .add("chapter" -> PuzzleTheme.studyChapterIds.get(theme.key))
        )
        .add("user" -> user.map(userJson))
        .add("replay" -> replay.map(replayJson))
    }
  }

  def otherSourcesJson(puzzle: Puzzle) =
    Json
      .obj(
        "sfen" -> puzzle.sfen
      )
      .add("author" -> puzzle.author)
      .add("description" -> puzzle.description)

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
        "ratingDiff" -> (perf.intRating - u.perfs.puzzle.intRating)
      )
      .add("vote" -> round.vote)
      .add("themes" -> round.nonEmptyThemes.map { rt =>
        JsObject(rt.map { t =>
          t.theme.value -> JsBoolean(t.vote)
        })
      })

  def pref(p: lila.pref.Pref) =
    Json.obj(
      "blindfold"          -> p.blindfold,
      "coords"             -> p.coords,
      "animation"          -> Json.obj("duration" -> p.animationMillis),
      "destination"        -> p.destination,
      "dropDestination"    -> p.dropDestination,
      "moveEvent"          -> p.moveEvent,
      "highlightLastDests" -> p.highlightLastDests,
      "highlightCheck"     -> p.highlightCheck,
      "squareOverlay"      -> p.squareOverlay,
      "resizeHandle"       -> p.resizeHandle,
      "keyboardMove"       -> (p.keyboardMove == lila.pref.Pref.KeyboardMove.YES)
    )

  def dashboardJson(dash: PuzzleDashboard, days: Int)(implicit lang: Lang) = Json.obj(
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
    "firstWins"       -> res.wins,
    "replayWins"      -> res.fixed,
    "puzzleRatingAvg" -> res.puzzleRatingAvg,
    "performance"     -> res.performance
  )

  private def puzzleJson(puzzle: Puzzle): JsObject = Json
    .obj(
      "id"         -> puzzle.id,
      "rating"     -> puzzle.glicko.intRating,
      "plays"      -> puzzle.plays,
      "initialPly" -> puzzle.initialPly,
      "solution" -> {
        if (puzzle.gameId.isDefined) puzzle.line.tail.map(_.usi).toList
        else puzzle.line.map(_.usi).toList
      },
      "themes" -> simplifyThemes(puzzle.themes)
    )
    .add("ambPromotions", puzzle.ambiguousPromotions.some.filter(_.nonEmpty))

  private def simplifyThemes(themes: Set[PuzzleTheme.Key]) =
    themes.filterNot(_ == PuzzleTheme.mate.key)

  object bc {

    def apply(puzzle: Puzzle, user: Option[User])(implicit
        lang: Lang
    ): Fu[JsObject] = {
      puzzle.gameId.fold(fuccess(otherSourcesJson(puzzle))) { gid =>
        gameJson(
          gameId = gid,
          plies = puzzle.initialPly,
          bc = true
        )
      } map { gameJson =>
        Json
          .obj(
            "game"   -> gameJson,
            "puzzle" -> puzzleJson(puzzle)
          )
          .add("user" -> user.map(_.perfs.puzzle.intRating).map(userJson))
      }
    }

    def batch(puzzles: Seq[Puzzle], user: Option[User])(implicit
        lang: Lang
    ): Fu[JsObject] = for {
      games <- gameRepo.gameOptionsFromSecondary(puzzles.map(_.gameId.get))
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

    def userJson(rating: Int) = Json.obj(
      "rating" -> rating,
      "recent" -> Json.arr()
    )

    private def puzzleJson(puzzle: Puzzle) = Json.obj(
      "id"         -> Puzzle.numericalId(puzzle.id),
      "realId"     -> puzzle.id,
      "rating"     -> puzzle.glicko.intRating,
      "attempts"   -> puzzle.plays,
      "sfen"       -> puzzle.sfen,
      "color"      -> puzzle.color.name,
      "initialPly" -> (puzzle.initialPly + 1),
      "gameId"     -> puzzle.gameId,
      "lines" -> {
        if (puzzle.gameId.isDefined)
          puzzle.line.tail.reverse.foldLeft[JsValue](JsString("win")) { case (acc, move) =>
            Json.obj(move.usi -> acc)
          }
        else
          puzzle.line.reverse.foldLeft[JsValue](JsString("win")) { case (acc, move) =>
            Json.obj(move.usi -> acc)
          }
      },
      "vote"   -> 0,
      "branch" -> makeBranch(puzzle).map(defaultNodeJsonWriter.writes)
    )

    private def makeBranch(puzzle: Puzzle): Option[tree.Branch] = {
      val init =
        shogi.Game(puzzle.sfenAfterInitialMove.some, shogi.variant.Standard).withPlies(puzzle.initialPly + 1)
      val solution = puzzle.gameId.fold(puzzle.line.toList)(_ => puzzle.line.tail)
      val (_, branchList) = solution.foldLeft[(shogi.Game, List[tree.Branch])]((init, Nil)) {
        case ((prev, branches), usi) =>
          val game =
            prev(usi).getOrElse(prev) // fold(err => sys error s"puzzle ${puzzle.id} $err", identity) // todo
          val branch = tree.Branch(
            id = shogi.format.usi.UsiCharPair(usi, shogi.variant.Standard),
            ply = game.plies,
            usi = usi,
            sfen = game.toSfen,
            check = game.situation.check
          )
          (game, branch :: branches)
      }
      branchList.foldLeft[Option[tree.Branch]](None) {
        case (None, branch)        => branch.some
        case (Some(child), branch) => Some(branch addChild child)
      }
    }
  }
}

object JsonView {

  implicit val puzzleIdWrites: Writes[Puzzle.Id] = stringIsoWriter(Puzzle.idIso)

  implicit val puzzleThemeKeyWrites: Writes[PuzzleTheme.Key] = stringIsoWriter(PuzzleTheme.keyIso)
}
