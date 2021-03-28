package views
package html.puzzle

import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json.{ JsArray, JsObject, JsString, Json }

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.i18n.{ JsDump, MessageKey }
import lila.puzzle.{ PuzzleDifficulty, PuzzleTheme }

object bits {

  private val dataLastmove = attr("data-lastmove")

  def daily(p: lila.puzzle.Puzzle, fen: chess.format.FEN, lastMove: String) =
    views.html.board.bits.mini(fen, p.color, lastMove)(span)

  def jsI18n(streak: Boolean)(implicit lang: Lang) = i18nJsObject {
    i18nKeys ++ streak.??(streakI18nKeys)
  } +
    (PuzzleTheme.enPassant.key.value -> JsString(PuzzleTheme.enPassant.name.txt()(lila.i18n.defaultLang)))

  lazy val jsonThemes = PuzzleTheme.all
    .collect {
      case t if t != PuzzleTheme.mix => t.key
    }
    .partition(PuzzleTheme.staticThemes.contains) match {
    case (static, dynamic) =>
      Json.obj(
        "dynamic" -> dynamic.map(_.value).sorted.mkString(" "),
        "static"  -> static.map(_.value).mkString(" ")
      )
  }

  def pageMenu(active: String, days: Int = 30)(implicit lang: Lang) =
    st.nav(cls := "page-menu__menu subnav")(
      a(href := routes.Puzzle.home)(
        trans.puzzles()
      ),
      a(cls := active.active("themes"), href := routes.Puzzle.themes)(
        trans.puzzle.puzzleThemes()
      ),
      a(cls := active.active("dashboard"), href := routes.Puzzle.dashboard(days, "dashboard"))(
        trans.puzzle.puzzleDashboard()
      ),
      a(cls := active.active("improvementAreas"), href := routes.Puzzle.dashboard(days, "improvementAreas"))(
        trans.puzzle.improvementAreas()
      ),
      a(cls := active.active("strengths"), href := routes.Puzzle.dashboard(days, "strengths"))(
        trans.puzzle.strengths()
      ),
      a(cls := active.active("history"), href := routes.Puzzle.history(1))(
        trans.puzzle.history()
      ),
      a(cls := active.active("player"), href := routes.Puzzle.ofPlayer())(
        "From my games"
      )
    )

  private val i18nKeys: List[MessageKey] = {
    List(
      trans.puzzle.yourPuzzleRatingX,
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
      trans.puzzle.didYouLikeThisPuzzle,
      trans.puzzle.voteToLoadNextOne,
      trans.puzzle.puzzleId,
      trans.puzzle.ratingX,
      trans.puzzle.playedXTimes,
      trans.puzzle.continueTraining,
      trans.puzzle.difficultyLevel,
      trans.puzzle.example,
      trans.puzzle.toGetPersonalizedPuzzles,
      trans.puzzle.addAnotherTheme,
      trans.signUp,
      trans.analysis,
      trans.playWithTheMachine,
      // ceval
      trans.depthX,
      trans.usingServerAnalysis,
      trans.loadingEngine,
      trans.cloudAnalysis,
      trans.goDeeper,
      trans.showThreat,
      trans.gameOver,
      trans.inLocalBrowser,
      trans.toggleLocalEvaluation
    ) ::: PuzzleTheme.all.map(_.name) :::
      PuzzleTheme.all.map(_.description) :::
      PuzzleDifficulty.all.map(_.name)
  }.map(_.key)

  private val streakI18nKeys: List[MessageKey] =
    List(
      trans.storm.skip,
      trans.puzzle.yourStreakX,
      trans.puzzle.streakSkipExplanation,
      trans.puzzle.continueTheStreak,
      trans.puzzle.newStreak
    ).map(_.key)
}
