package views
package html.puzzle

import chess.format.{ BoardFen, Uci }
import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json.Json

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.puzzle.{ PuzzleDifficulty, PuzzleTheme }
import lila.user.User
import lila.core.i18n.I18nKey

object bits:

  def daily(p: lila.puzzle.Puzzle, fen: BoardFen, lastMove: Uci) =
    views.html.board.bits.mini(fen, p.color, lastMove.some)(span)

  def jsI18n(streak: Boolean)(using Translate) =
    if streak then i18nJsObject(streakI18nKeys)
    else i18nJsObject(trainingI18nKeys)

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
        trans.site.puzzles()
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

  private val themeI18nKeys: List[I18nKey] =
    PuzzleTheme.visible.map(_.name) ::: PuzzleTheme.visible.map(_.description)

  private val baseI18nKeys: List[I18nKey] = List(
    trans.puzzle.bestMove,
    trans.puzzle.keepGoing,
    trans.puzzle.notTheMove,
    trans.puzzle.trySomethingElse,
    trans.site.yourTurn,
    trans.puzzle.findTheBestMoveForBlack,
    trans.puzzle.findTheBestMoveForWhite,
    trans.site.viewTheSolution,
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
    trans.site.analysis,
    trans.site.playWithTheMachine,
    trans.preferences.zenMode,
    trans.site.asWhite,
    trans.site.asBlack,
    trans.site.randomColor,
    trans.site.flipBoard
  ) ::: views.html.board.userAnalysisI18n.cevalTranslations.toList

  private val trainingI18nKeys: List[I18nKey] = baseI18nKeys ::: List[I18nKey](
    trans.puzzle.example,
    trans.puzzle.dailyPuzzle,
    trans.puzzle.addAnotherTheme,
    trans.puzzle.difficultyLevel,
    trans.site.rated,
    trans.puzzle.yourPuzzleRatingWillNotChange,
    trans.site.signUp,
    trans.puzzle.toGetPersonalizedPuzzles,
    trans.puzzle.nbPointsBelowYourPuzzleRating,
    trans.puzzle.nbPointsAboveYourPuzzleRating
  ) :::
    themeI18nKeys :::
    PuzzleDifficulty.all.map(_.name)

  private val streakI18nKeys: List[I18nKey] = baseI18nKeys ::: List[I18nKey](
    trans.storm.skip,
    trans.puzzle.streakDescription,
    trans.puzzle.yourStreakX,
    trans.puzzle.streakSkipExplanation,
    trans.puzzle.continueTheStreak,
    trans.puzzle.newStreak
  ) ::: themeI18nKeys
