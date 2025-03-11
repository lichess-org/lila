package lila.puzzle

import chess.format.*
import chess.IntRating
import chess.rating.IntRatingDiff
import scalalib.model.Days
import play.api.libs.json.*

import lila.common.Json.given
import lila.core.i18n.{ Translate, Translator }
import lila.tree.{ Metas, NewBranch, NewTree }
import lila.core.net.ApiVersion
import lila.ui.Context

final class JsonView(
    gameJson: GameJson,
    gameRepo: lila.core.game.GameRepo,
    myEngines: lila.core.misc.analysis.MyEnginesAsJson
)(using Executor, Translator):

  import JsonView.{ *, given }

  def apply(
      puzzle: Puzzle,
      angle: Option[PuzzleAngle],
      replay: Option[PuzzleReplay]
  )(using Translate)(using Option[Me], Perf): Fu[JsObject] =
    gameJson(
      gameId = puzzle.gameId,
      plies = puzzle.initialPly,
      bc = false
    ).map: gameJson =>
      puzzleAndGamejson(puzzle, gameJson)
        .add("user" -> userJson)
        .add("replay" -> replay.map(replayJson))
        .add(
          "angle",
          angle.map: a =>
            Json
              .toJsObject(a)
              .add("chapter" -> a.asTheme.flatMap(PuzzleTheme.studyChapterIds.get))
              .add("opening" -> a.opening.map: op =>
                Json.obj("key" -> op.key, "name" -> op.name))
              .add("openingAbstract" -> a.match
                case op: PuzzleAngle.Opening => op.isAbstract
                case _                       => false)
        )

  def analysis(
      puzzle: Puzzle,
      angle: PuzzleAngle,
      replay: Option[lila.puzzle.PuzzleReplay] = None,
      newMe: Option[Me] = None,
      apiVersion: Option[ApiVersion] = None
  )(using ctx: Context)(using Perf, Translate): Fu[JsObject] =
    given me: Option[Me] = newMe.orElse(ctx.me)
    for
      puzzleJson <-
        if apiVersion.exists(v => !ApiVersion.puzzleV2(v))
        then bc(puzzle)
        else apply(puzzle, angle.some, replay)
      enginesJson <- myEngines.get(me)
    yield puzzleJson ++ enginesJson

  def userJson(using me: Option[Me], perf: Perf) = me.map: me =>
    Json
      .obj(
        "id"     -> me.userId,
        "rating" -> perf.intRating
      )
      .add("provisional" -> perf.provisional)

  private def replayJson(r: PuzzleReplay) =
    Json.obj("days" -> r.days, "i" -> r.i, "of" -> r.nb)

  object roundJson:
    def web(round: PuzzleRound, perf: Perf)(using prevPerf: Perf) =
      base(round, (perf.intRating - prevPerf.intRating).into(IntRatingDiff))
        .add("vote" -> round.vote)
        .add("themes" -> round.nonEmptyThemes.map: rt =>
          JsObject:
            rt.map: t =>
              t.theme.value -> JsBoolean(t.vote))

    def api = base
    private def base(round: PuzzleRound, ratingDiff: IntRatingDiff) = Json.obj(
      "id"         -> round.id.puzzleId,
      "win"        -> round.win,
      "ratingDiff" -> ratingDiff
    )

  def pref(p: lila.core.pref.Pref) =
    Json.obj(
      "coords"       -> p.coords,
      "keyboardMove" -> p.keyboardMove,
      "voiceMove"    -> p.voice,
      "rookCastle"   -> p.rookCastle,
      "animation"    -> Json.obj("duration" -> p.animationMillis),
      "destination"  -> p.destination,
      "moveEvent"    -> p.moveEvent,
      "highlight"    -> p.highlight,
      "is3d"         -> p.is3d
    )

  def dashboardJson(dash: PuzzleDashboard, days: Days)(using Translate) = Json.obj(
    "days"   -> days,
    "global" -> dashboardResults(dash.global),
    "themes" -> JsObject(dash.byTheme.toList.sortBy(-_._2.nb).map { (key, res) =>
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

  def batch(puzzles: Seq[Puzzle])(using me: Option[Me], perf: Perf): Fu[JsObject] = for
    games <- gameRepo.gameOptionsFromSecondary(puzzles.map(_.gameId))
    jsons <- Future.sequence:
      puzzles
        .zip(games)
        .collect { case (puzzle, Some(game)) =>
          gameJson
            .noCache(game, puzzle.initialPly)
            .map:
              puzzleAndGamejson(puzzle, _)
        }
  yield
    import lila.rating.Glicko.glickoWrites
    Json.obj("puzzles" -> jsons).add("glicko" -> me.map(_ => perf.glicko))

  object bc:

    def apply(puzzle: Puzzle)(using me: Option[Me], perf: Perf): Fu[JsObject] =
      gameJson(gameId = puzzle.gameId, plies = puzzle.initialPly, bc = true).map: gameJson =>
        Json
          .obj(
            "game"   -> gameJson,
            "puzzle" -> puzzleJson(puzzle)
          )
          .add("user" -> me.map(_ => perf.intRating).map(userJson))

    def batch(puzzles: Seq[Puzzle])(using me: Option[Me], perf: Perf): Fu[JsObject] = for
      games <- gameRepo.gameOptionsFromSecondary(puzzles.map(_.gameId))
      jsons <- Future.sequence:
        puzzles
          .zip(games)
          .collect { case (puzzle, Some(game)) =>
            gameJson.noCacheBc(game, puzzle.initialPly).map { gameJson =>
              Json.obj(
                "game"   -> gameJson,
                "puzzle" -> puzzleJson(puzzle)
              )
            }
          }
    yield Json
      .obj("puzzles" -> jsons)
      .add("user" -> me.map(_ => perf.intRating).map(userJson))

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
      "lines" -> puzzle.line.tail.reverse.foldLeft[JsValue](JsString("win")): (acc, move) =>
        Json.obj(move.uci -> acc),
      "vote"   -> 0,
      "branch" -> makeTree(puzzle).map(NewTree.defaultNodeJsonWriter.writes)
    )

object JsonView:

  given (using Translate): OWrites[PuzzleAngle] = a =>
    Json
      .obj(
        "key" -> a.key,
        "name" -> {
          if a == PuzzleAngle.mix
          then lila.core.i18n.I18nKey.puzzle.puzzleThemes.txt()
          else a.name.txt()
        },
        "desc" -> a.description.txt()
      )

  given OWrites[PuzzleReplay] = Json.writes[PuzzleReplay]

  def makeTree(puzzle: Puzzle): Option[NewTree] =

    def makeNode(prev: chess.Game, uci: Uci.Move): (chess.Game, NewTree) =
      val (game, move) = prev(uci.orig, uci.dest, uci.promotion)
        .fold(err => sys.error(s"puzzle ${puzzle.id} $err"), identity)
      game -> chess.Node(
        NewBranch(
          id = UciCharPair(move.toUci),
          move = Uci.WithSan(move.toUci, game.sans.last),
          metas = Metas(
            fen = Fen.write(game),
            check = game.situation.check,
            ply = game.ply,
            crazyData = none
          )
        )
      )

    chess.Tree.buildAccumulate(puzzle.line.tail, puzzle.initialGame, makeNode)

  def puzzleAndGamejson(puzzle: Puzzle, game: JsObject) = Json.obj(
    "game" -> game,
    "puzzle" -> puzzleJsonBase(puzzle).++ {
      Json.obj("initialPly" -> puzzle.initialPly)
    }
  )

  def puzzleJsonStandalone(puzzle: Puzzle): JsObject =
    puzzleJsonBase(puzzle) ++ Json.obj(
      "fen"      -> puzzle.fenAfterInitialMove,
      "lastMove" -> puzzle.line.head.uci
    )

  private def puzzleJsonBase(puzzle: Puzzle): JsObject = Json.obj(
    "id"       -> puzzle.id,
    "rating"   -> puzzle.glicko.intRating,
    "plays"    -> puzzle.plays,
    "solution" -> puzzle.line.tail.map(_.uci),
    "themes"   -> simplifyThemes(puzzle.themes)
  )
  private def simplifyThemes(themes: Set[PuzzleTheme.Key]) =
    themes.filterNot(_ == PuzzleTheme.mate.key)

  def angles(all: PuzzleAngle.All)(using Translate) = Json.obj(
    "themes" -> JsObject:
      all.themes.map: (i18n, themes) =>
        i18n.txt() -> JsArray:
          themes.map:
            case PuzzleTheme.WithCount(theme, count) =>
              Json.obj(
                "key"   -> theme.key,
                "name"  -> theme.name.txt(),
                "desc"  -> theme.description.txt(),
                "count" -> count
              )
  )

  def openings(all: PuzzleOpeningCollection, mine: Option[PuzzleOpening.Mine])(using Translate): JsObject =
    Json.obj(
      "openings" ->
        all
          .treeList(lila.puzzle.PuzzleOpening.Order.Popular)
          .map: (fam, ops) =>
            Json.obj(
              "family" -> Json.obj(
                "key"   -> fam.family.key,
                "name"  -> fam.family.name,
                "count" -> fam.count
              ),
              "openings" -> ops.map: op =>
                Json.obj(
                  "key"   -> op.opening.key,
                  "name"  -> op.opening.variation,
                  "count" -> op.count
                )
            )
    )
