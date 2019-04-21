package views.html.puzzle

import play.api.mvc.RequestHeader

import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.app.ui.EmbedConfig
import views.html.base.layout.{ bits => layout }

import controllers.routes

object embed {

  import EmbedConfig.implicits._

  private val dataStreamUrl = attr("data-stream-url")

  def apply(daily: lidraughts.puzzle.DailyPuzzle)(implicit config: EmbedConfig) = frag(
    layout.doctype,
    layout.htmlTag(config.lang)(
      head(
        layout.charset,
        layout.metaCsp(basicCsp),
        st.headTitle("lidraughts.org draughts puzzle"),
        layout.pieceSprite(lidraughts.pref.PieceSet.default),
        cssTagWithTheme("tv.embed", config.bg)
      ),
      body(
        cls := s"base ${config.board}",
        dataStreamUrl := routes.Tv.feed
      )(
          div(id := "daily-puzzle", cls := "embedded", title := trans.clickToSolve.txt())(
            raw(daily.html),
            div(cls := "vstext", style := "text-align: center; justify-content: center")(
              trans.puzzleOfTheDay.frag(), br,
              daily.color.fold(trans.whitePlays, trans.blackPlays).frag()
            )
          ),
          jQueryTag,
          jsAt("javascripts/vendor/draughtsground.min.js", false),
          jsAt("compiled/puzzle.js", false)
        )
    )
  )
}
