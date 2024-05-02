package views.puzzle

import scalalib.paginator.Paginator

import lila.app.UiEnv.{ *, given }
import lila.puzzle.{ Puzzle, PuzzleOpening, DailyPuzzle }

lazy val bits = lila.puzzle.ui.PuzzleBits(helpers)(views.userAnalysisI18n.cevalTranslations)
lazy val ui   = lila.puzzle.ui.PuzzleUi(helpers, bits)

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
