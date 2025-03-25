package views.puzzle
import lila.app.UiEnv.{ *, given }
import lila.puzzle.DailyPuzzle

lazy val bits = lila.puzzle.ui.PuzzleBits(helpers)
lazy val ui =
  lila.puzzle.ui
    .PuzzleUi(helpers, bits)(views.analyse.ui.bits.cspExternalEngine, analyseEndpoints.externalEngine)

def embed(daily: DailyPuzzle.WithHtml)(using config: EmbedContext) =
  views.base.embed.minimal(
    title = "lichess.org chess puzzle",
    cssKeys = List("bits.tv.embed"),
    modules = Esm("site.puzzleEmbed")
  )(
    bits.dailyLink(daily)(using config.translate)(
      targetBlank,
      id  := "daily-puzzle",
      cls := "embedded"
    ),
    chessgroundTag
  )
