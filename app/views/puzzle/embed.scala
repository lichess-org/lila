package views.html.puzzle

import controllers.routes
import play.api.i18n.Lang

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._
import lila.puzzle.DailyPuzzle

object embed {

  import EmbedConfig.implicits.configLang

  def apply(daily: DailyPuzzle.WithHtml)(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = s"lishogi.org - ${trans.puzzles.txt()}",
      moreCss = cssTag("embed.puzzle"),
      moreJs = jsTag("embed.puzzle")
    )(
      dailyLink(daily)(config.lang)(
        targetBlank,
        id  := "daily-puzzle",
        cls := "embedded"
      )
    )

  def dailyLink(daily: DailyPuzzle.WithHtml)(implicit lang: Lang) = a(
    href  := routes.Puzzle.daily,
    title := trans.puzzle.clickToSolve.txt()
  )(
    raw(daily.html),
    div(cls := "vstext")(
      trans.puzzle.puzzleOfTheDay(),
      br,
      trans.xPlays(daily.puzzle.color.fold(trans.sente, trans.gote)())
    )
  )
}
