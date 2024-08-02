package views.puzzle
import lila.app.UiEnv.{ *, given }
import lila.puzzle.DailyPuzzle

lazy val bits = lila.puzzle.ui.PuzzleBits(helpers)(views.userAnalysisI18n.cevalTranslations)
lazy val ui   = lila.puzzle.ui.PuzzleUi(helpers, bits)(views.analyse.ui.csp, externalEngineEndpoint)

def embed(daily: DailyPuzzle.WithHtml)(using config: EmbedContext) =
  views.base.embed.minimal(
    title = "lichess.org chess puzzle",
    cssKeys = List("bits.tv.embed"),
    modules = EsmInit("site.puzzleEmbed")
  )(
    bits.dailyLink(daily)(using config.translate)(
      targetBlank,
      id  := "daily-puzzle",
      cls := "embedded"
    ),
    chessgroundTag
  )
