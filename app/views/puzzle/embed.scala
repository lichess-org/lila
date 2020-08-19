package views.html.puzzle

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.app.ui.EmbedConfig
import views.html.base.layout.{ bits => layout }

import controllers.routes

object embed {

  import EmbedConfig.implicits._

  private val dataStreamUrl = attr("data-stream-url")

  def apply(daily: lila.puzzle.DailyPuzzle)(implicit config: EmbedConfig) =
    frag(
      layout.doctype,
      layout.htmlTag(config.lang)(
        head(
          layout.charset,
          layout.metaCsp(basicCsp),
          st.headTitle("lichess.org chess puzzle"),
          layout.pieceSprite(lila.pref.PieceSet.default),
          cssTagWithTheme("tv.embed", config.bg)
        ),
        body(
          cls := s"base ${config.board}",
          dataStreamUrl := routes.Tv.feed()
        )(
          a(
            href := routes.Puzzle.daily(),
            id := "daily-puzzle",
            cls := "embedded",
            title := trans.clickToSolve.txt()
          )(
            span(cls := "text")(trans.puzzleOfTheDay()),
            raw(daily.html),
            span(cls := "text")(daily.color.fold(trans.whitePlays, trans.blackPlays)())
          ),
          jQueryTag,
          jsAt("javascripts/vendor/chessground.min.js", defer = false),
          jsAt("compiled/puzzle.js", defer = false)
        )
      )
    )
}
