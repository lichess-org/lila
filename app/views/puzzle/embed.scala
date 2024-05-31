package views.html.puzzle

import controllers.routes
import play.api.i18n.Lang

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._
import lila.puzzle.DailyPuzzle

object embed {

  def apply(daily: DailyPuzzle.WithHtml)(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = "lishogi.org shogi puzzle",
      cssModule = "tv.embed"
    )(
      dailyLink(daily)(config.lang)(
        targetBlank,
        id  := "daily-puzzle",
        cls := "embedded"
      ),
      jQueryTag,
      jsAt("javascripts/vendor/shogiground.min.js", false),
      jsAt("compiled/puzzle.js", false)
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
