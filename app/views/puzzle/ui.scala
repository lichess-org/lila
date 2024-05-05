package views.puzzle

import scalalib.paginator.Paginator

import lila.app.UiEnv.{ *, given }
import lila.puzzle.{ Puzzle, PuzzleOpening, DailyPuzzle }

lazy val bits = lila.puzzle.ui.PuzzleBits(helpers)(views.userAnalysisI18n.cevalTranslations)
lazy val ui   = lila.puzzle.ui.PuzzleUi(helpers, bits)(views.analyse.ui.csp)

def embed(daily: DailyPuzzle.WithHtml)(using config: EmbedContext) =
  views.base.embed(
    title = "lichess.org chess puzzle",
    cssModule = "tv.embed",
    modules = EsmInit("site.puzzleEmbed")
  )(
    bits.dailyLink(daily)(using config.translate)(
      targetBlank,
      id  := "daily-puzzle",
      cls := "embedded"
    ),
    chessgroundTag
  )
