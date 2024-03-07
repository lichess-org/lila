package views
package html.puzzle

import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json.Json

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.i18n.MessageKey
import lila.puzzle.{ PuzzleDifficulty, PuzzleTheme }

object bits {

  private val dataLastUsi = attr("data-lastmove")

  def miniTag(sfen: shogi.format.forsyth.Sfen, color: shogi.Color = shogi.Sente, lastUsi: String = "")(
      tag: Tag
  ): Tag =
    tag(
      cls         := "mini-board parse-sfen",
      dataColor   := color.name,
      dataSfen    := sfen.value,
      dataLastUsi := lastUsi
    )(div(cls     := s"sg-wrap d-9x9 orientation-${color.name}"))

  def daily(p: lila.puzzle.Puzzle, sfen: shogi.format.forsyth.Sfen, lastUsi: String) =
    miniTag(sfen, p.color, lastUsi)(span)

  def jsI18n(implicit lang: Lang) = i18nJsObject(i18nKeys)

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
      a(href := routes.Puzzle.show("tsume"))(
        trans.puzzleTheme.tsume()
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
        trans.puzzle.fromMyGames()
      ),
      a(cls := active.active("submitted"), href := routes.Puzzle.submitted())(
        trans.puzzle.submissions()
      )
    )

  private val i18nKeys: List[MessageKey] = {
    List(
      trans.black,
      trans.white,
      trans.sente,
      trans.gote,
      trans.shitate,
      trans.uwate,
      trans.puzzle.yourPuzzleRatingX,
      trans.puzzle.bestMove,
      trans.mistake,
      trans.puzzle.keepGoing,
      trans.puzzle.notTheMove,
      trans.puzzle.trySomethingElse,
      trans.yourTurn,
      trans.puzzle.findTheBestMoveForX,
      trans.viewTheSolution,
      trans.puzzle.puzzleSuccess,
      trans.puzzle.puzzleComplete,
      trans.puzzle.hidden,
      trans.puzzle.jumpToNextPuzzleImmediately,
      trans.puzzle.fromGameLink,
      trans.puzzle.puzzleSource,
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
      trans.pressXtoFocus,
      trans.pressXtoSubmit,
      trans.levelX,
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
}
