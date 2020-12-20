package views.html.puzzle

import controllers.routes

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._

object embed {

  import EmbedConfig.implicits._

  def apply(daily: lila.puzzle.DailyPuzzle)(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = "lichess.org chess puzzle",
      cssModule = "tv.embed"
    )(
      a(
        href := routes.Puzzle.daily(),
        target := "_blank",
        id := "daily-puzzle",
        cls := "embedded",
        title := trans.clickToSolve.txt()
      )(
        span(cls := "text")(trans.puzzleOfTheDay()),
        raw(daily.html),
        span(cls := "text")(daily.color.fold(trans.whitePlays, trans.blackPlays)())
      ),
      jsModule("puzzle.embed")
    )
}
