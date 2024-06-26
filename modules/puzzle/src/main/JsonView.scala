package lila.puzzle

import play.api.i18n.Lang
import play.api.libs.json._

import lila.common.Json._
import lila.rating.Perf
import lila.user.User

final class JsonView(
    gameJson: GameJson
)(implicit ec: scala.concurrent.ExecutionContext) {

  import JsonView._

  def apply(puzzle: Puzzle, theme: PuzzleTheme, replay: Option[PuzzleReplay], user: Option[User])(implicit
      lang: Lang
  ): Fu[JsObject] = {
    puzzle.gameId.fold(fuccess(otherSourcesJson(puzzle))) { gid =>
      gameJson(
        gameId = gid,
        plies = puzzle.initialPly
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

  private def otherSourcesJson(puzzle: Puzzle) =
    Json
      .obj(
        "sfen" -> puzzle.sfen
      )
      .add("author" -> puzzle.author)
      .add("description" -> puzzle.description)

  private def userJson(u: User) =
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

  def roundJsonApi(round: PuzzleRound, ratingDiff: IntRatingDiff) = Json.obj(
    "id"         -> round.id.puzzleId,
    "win"        -> round.win,
    "ratingDiff" -> ratingDiff.value
  )

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
      "id"          -> puzzle.id,
      "rating"      -> puzzle.glicko.intRating,
      "plays"       -> puzzle.plays,
      "initialSfen" -> puzzle.sfen,
      "initialPly"  -> puzzle.initialPly,
      "solution" -> {
        if (puzzle.gameId.isDefined) puzzle.line.tail.map(_.usi).toList
        else puzzle.line.map(_.usi).toList
      },
      "themes" -> simplifyThemes(puzzle.themes)
    )
    .add("ambPromotions", puzzle.ambiguousPromotions.some.filter(_.nonEmpty))
    .add("initialUsi", puzzle.gameId.isDefined option puzzle.line.head.usi)

  private def simplifyThemes(themes: Set[PuzzleTheme.Key]) =
    themes.filterNot(_ == PuzzleTheme.mate.key)

  object mobile {

    def apply(puzzles: Seq[Puzzle], theme: PuzzleTheme, user: Option[User]): JsObject =
      Json
        .obj("puzzles" -> puzzles.map(p => mobilePuzzleJson(p)), "theme" -> theme.key)
        .add("user" -> user.map(userJson))

    private def mobileSource(puzzle: Puzzle): JsObject =
      Json.obj(
        "gameId" -> puzzle.gameId,
        "author" -> puzzle.author
      )

    private def mobilePuzzleJson(puzzle: Puzzle): JsObject =
      puzzleJson(puzzle) ++ mobileSource(puzzle)

  }
}

object JsonView {

  implicit val puzzleIdWrites: Writes[Puzzle.Id] = stringIsoWriter(Puzzle.idIso)

  implicit val puzzleThemeKeyWrites: Writes[PuzzleTheme.Key] = stringIsoWriter(PuzzleTheme.keyIso)
}
