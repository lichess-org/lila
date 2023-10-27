package views
package html.puzzle

import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json.{ JsString, Json }

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.puzzle.{ PuzzleDifficulty, PuzzleTheme }
import chess.format.{ BoardFen, Uci }
import lila.user.User

object bits:

  def daily(p: lila.puzzle.Puzzle, fen: BoardFen, lastMove: Uci) =
    views.html.board.bits.mini(fen, p.color, lastMove.some)(span)

  def jsI18n(streak: Boolean)(using Lang) =
    if streak then i18nJsObject(streakI18nKeys)
    else
      i18nJsObject(trainingI18nKeys) + {
        PuzzleTheme.enPassant.key.value -> JsString(PuzzleTheme.enPassant.name.txt())
      }

  lazy val jsonThemes = PuzzleTheme.visible
    .collect { case t if t != PuzzleTheme.mix => t.key }
    .partition(PuzzleTheme.staticThemes.contains) match
    case (static, dynamic) =>
      Json.obj(
        "dynamic" -> dynamic.sorted(stringOrdering).mkString(" "),
        "static"  -> static.mkString(" ")
      )

  def pageMenu(active: String, user: Option[User], days: Int = 30)(using ctx: PageContext) =
    val u = user.filterNot(ctx.is).map(_.username)
    views.html.site.bits.pageMenuSubnav(
      a(href := routes.Puzzle.home)(
        trans.puzzles()
      ),
      a(cls := active.active("themes"), href := routes.Puzzle.themes)(
        trans.puzzle.puzzleThemes()
      ),
      a(cls := active.active("openings"), href := routes.Puzzle.openings())(
        trans.puzzle.byOpenings()
      ),
      a(cls := active.active("dashboard"), href := routes.Puzzle.dashboard(days, "dashboard", u))(
        trans.puzzle.puzzleDashboard()
      ),
      a(
        cls  := active.active("improvementAreas"),
        href := routes.Puzzle.dashboard(days, "improvementAreas", u)
      )(
        trans.puzzle.improvementAreas()
      ),
      a(cls := active.active("strengths"), href := routes.Puzzle.dashboard(days, "strengths", u))(
        trans.puzzle.strengths()
      ),
      a(cls := active.active("history"), href := routes.Puzzle.history(1, u))(
        trans.puzzle.history()
      ),
      a(cls := active.active("player"), href := routes.Puzzle.ofPlayer())(
        trans.puzzle.fromMyGames()
      )
    )

  private val baseI18nKeys = List(
    trans.puzzle.bestMove,
    trans.puzzle.keepGoing,
    trans.puzzle.notTheMove,
    trans.puzzle.trySomethingElse,
    trans.yourTurn,
    trans.puzzle.findTheBestMoveForBlack,
    trans.puzzle.findTheBestMoveForWhite,
    trans.viewTheSolution,
    trans.puzzle.puzzleSuccess,
    trans.puzzle.puzzleComplete,
    trans.puzzle.hidden,
    trans.puzzle.jumpToNextPuzzleImmediately,
    trans.puzzle.fromGameLink,
    trans.puzzle.puzzleId,
    trans.puzzle.ratingX,
    trans.puzzle.playedXTimes,
    trans.puzzle.continueTraining,
    trans.puzzle.didYouLikeThisPuzzle,
    trans.puzzle.voteToLoadNextOne,
    trans.analysis,
    trans.playWithTheMachine,
    trans.preferences.zenMode,
    trans.asWhite,
    trans.asBlack,
    trans.randomColor,
    // ceval
    trans.depthX,
    trans.usingServerAnalysis,
    trans.loadingEngine,
    trans.calculatingMoves,
    trans.engineFailed,
    trans.cloudAnalysis,
    trans.goDeeper,
    trans.showThreat,
    trans.gameOver,
    trans.inLocalBrowser,
    trans.toggleLocalEvaluation,
    trans.flipBoard
  )

  private val trainingI18nKeys = baseI18nKeys ::: List(
    trans.puzzle.example,
    trans.puzzle.dailyPuzzle,
    trans.puzzle.addAnotherTheme,
    trans.puzzle.difficultyLevel,
    trans.rated,
    trans.puzzle.yourPuzzleRatingWillNotChange,
    trans.signUp,
    trans.puzzle.toGetPersonalizedPuzzles,
    trans.puzzle.nbPointsBelowYourPuzzleRating,
    trans.puzzle.nbPointsAboveYourPuzzleRating
  ) :::
    PuzzleTheme.visible.map(_.name) :::
    PuzzleTheme.visible.map(_.description) :::
    PuzzleDifficulty.all.map(_.name)

  private val streakI18nKeys = baseI18nKeys ::: List(
    trans.storm.skip,
    trans.puzzle.streakDescription,
    trans.puzzle.yourStreakX,
    trans.puzzle.streakSkipExplanation,
    trans.puzzle.continueTheStreak,
    trans.puzzle.newStreak
  )
