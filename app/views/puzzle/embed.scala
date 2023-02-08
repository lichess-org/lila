package views.html.puzzle

import controllers.routes
import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.puzzle.DailyPuzzle

object embed:

  import EmbedConfig.implicits.*

  def apply(daily: DailyPuzzle.WithHtml)(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = "lichess.org chess puzzle",
      cssModule = "tv.embed"
    )(
      dailyLink(daily)(config.lang)(
        targetBlank,
        id  := "daily-puzzle",
        cls := "embedded"
      ),
      jsModule("puzzle.embed")
    )

  def dailyLink(daily: DailyPuzzle.WithHtml)(implicit lang: Lang) = a(
    href  := routes.Puzzle.daily,
    title := trans.puzzle.clickToSolve.txt()
  )(
    span(cls := "text")(trans.puzzle.puzzleOfTheDay()),
    raw(daily.html),
    span(cls := "text")(daily.puzzle.color.fold(trans.whitePlays, trans.blackPlays)())
  )
