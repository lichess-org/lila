package views.puzzle

import scalalib.paginator.Paginator

import lila.app.templating.Environment.{ *, given }
import lila.puzzle.{ Puzzle, PuzzleOpening, DailyPuzzle }

lazy val bits = lila.puzzle.ui.PuzzleBits(helpers)(views.userAnalysisI18n.cevalTranslations)
lazy val ui   = lila.puzzle.ui.PuzzleUi(helpers, bits)

def openings(
    openings: lila.puzzle.PuzzleOpeningCollection,
    mine: Option[PuzzleOpening.Mine],
    order: PuzzleOpening.Order
)(using PageContext) =
  views.base.layout(
    title = trans.puzzle.puzzlesByOpenings.txt(),
    moreCss = cssTag("puzzle.page"),
    modules = EsmInit("puzzle.opening")
  )(ui.opening.all(openings, mine, order))

def themes(all: lila.puzzle.PuzzleAngle.All)(using PageContext) =
  views.base.layout(
    title = trans.puzzle.puzzleThemes.txt(),
    moreCss = cssTag("puzzle.page"),
    withHrefLangs = lila.ui.LangPath(routes.Puzzle.themes).some
  )(ui.theme.list(all))

def ofPlayer(query: String, user: Option[User], puzzles: Option[Paginator[Puzzle]])(using PageContext) =
  views.base.layout(
    title = user.fold(trans.puzzle.lookupOfPlayer.txt())(u => trans.puzzle.fromXGames.txt(u.username)),
    moreCss = cssTag("puzzle.page"),
    modules = infiniteScrollEsmInit
  )(ui.ofPlayer(query, user, puzzles))

def history(user: User, pager: Paginator[lila.puzzle.PuzzleHistory.PuzzleSession])(using ctx: PageContext) =
  val title =
    if ctx.is(user) then trans.puzzle.history.txt()
    else s"${user.username} ${trans.puzzle.history.txt()}"
  views.base.layout(
    title = title,
    moreCss = cssTag("puzzle.dashboard"),
    modules = infiniteScrollEsmInit
  )(ui.history(user, pager))

object embed:

  def apply(daily: DailyPuzzle.WithHtml)(using config: EmbedContext) =
    views.base.embed(
      title = "lichess.org chess puzzle",
      cssModule = "tv.embed",
      modules = EsmInit("site.puzzleEmbed")
    )(
      dailyLink(daily)(using config.translate)(
        targetBlank,
        id  := "daily-puzzle",
        cls := "embedded"
      ),
      chessgroundTag
    )

  def dailyLink(daily: DailyPuzzle.WithHtml)(using Translate) = a(
    href  := routes.Puzzle.daily,
    title := trans.puzzle.clickToSolve.txt()
  )(
    span(cls := "text")(trans.puzzle.puzzleOfTheDay()),
    rawHtml(daily.html),
    span(cls := "text")(daily.puzzle.color.fold(trans.site.whitePlays, trans.site.blackPlays)())
  )
