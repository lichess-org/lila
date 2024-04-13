package views.html.puzzle

import controllers.routes
import play.api.i18n.Lang

import lila.app.templating.Environment.*
import lila.web.ui.ScalatagsTemplate.{ *, given }
import lila.puzzle.DailyPuzzle

object embed:

  def apply(daily: DailyPuzzle.WithHtml)(using config: EmbedContext) =
    views.html.base.embed(
      title = "lichess.org chess puzzle",
      cssModule = "tv.embed"
    )(
      dailyLink(daily)(using config.translate)(
        targetBlank,
        id  := "daily-puzzle",
        cls := "embedded"
      ),
      chessgroundTag,
      jsTag("puzzle.embed")
    )

  def dailyLink(daily: DailyPuzzle.WithHtml)(using Translate) = a(
    href  := routes.Puzzle.daily,
    title := trans.puzzle.clickToSolve.txt()
  )(
    span(cls := "text")(trans.puzzle.puzzleOfTheDay()),
    rawHtml(daily.html),
    span(cls := "text")(daily.puzzle.color.fold(trans.site.whitePlays, trans.site.blackPlays)())
  )
