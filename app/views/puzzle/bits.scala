package views
package html.puzzle

import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json.{ JsString, Json }

import lila.api.{ Context, given }
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.i18n.MessageKey
import lila.puzzle.{ PuzzleDifficulty, PuzzleTheme }
import lila.user.User

object bits:

  private val dataLastmove = attr("data-lastmove")

  def daily(p: lila.puzzle.Puzzle, fen: chess.format.FEN, lastMove: String) =
    views.html.board.bits.mini(fen, p.color, lastMove)(span)

  def jsI18n(streak: Boolean)(implicit lang: Lang) =
    if (streak) i18nJsObject(streakI18nKeys)
    else
      i18nJsObject(trainingI18nKeys) + {
        PuzzleTheme.enPassant.key.value -> JsString(PuzzleTheme.enPassant.name.txt())
      }

  lazy val jsonThemes = PuzzleTheme.visible
    .collect { case t if t != PuzzleTheme.mix => t.key }
    .partition(PuzzleTheme.staticThemes.contains) match
    case (static, dynamic) =>
      Json.obj(
        "dynamic" -> dynamic.map(_.value).sorted.mkString(" "),
        "static"  -> static.mkString(" ")
      )

  def pageMenu(active: String, user: Option[User], days: Int = 30)(implicit ctx: Context) =
    val u = user.filterNot(ctx.is).map(_.username)
    st.nav(cls := "page-menu__menu subnav")(
      a(href := routes.Puzzle.home)(
        trans.puzzles()
      ),
      a(cls := active.active("themes"), href := routes.Puzzle.themes)(
        trans.puzzle.puzzleThemes()
      ),
      a(cls := active.active("openings"), href := routes.Puzzle.openings())(
        "By openings"
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

  private val baseI18nKeys: List[MessageKey] =
    List(
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
    ).map(_.key)

  private val trainingI18nKeys: List[MessageKey] =
    baseI18nKeys ::: List(
      trans.puzzle.example,
      trans.puzzle.addAnotherTheme,
      trans.puzzle.difficultyLevel,
      trans.rated,
      trans.puzzle.yourPuzzleRatingWillNotChange,
      trans.signUp,
      trans.puzzle.toGetPersonalizedPuzzles,
      trans.puzzle.nbPointsBelowYourPuzzleRating,
      trans.puzzle.nbPointsAboveYourPuzzleRating
    ).map(_.key) :::
      PuzzleTheme.visible.map(_.name.key) :::
      PuzzleTheme.visible.map(_.description.key) :::
      PuzzleDifficulty.all.map(_.name.key)

  private val streakI18nKeys: List[MessageKey] =
    baseI18nKeys ::: List(
      trans.storm.skip,
      trans.puzzle.streakDescription,
      trans.puzzle.yourStreakX,
      trans.puzzle.streakSkipExplanation,
      trans.puzzle.continueTheStreak,
      trans.puzzle.newStreak
    ).map(_.key)
