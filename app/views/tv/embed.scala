package views.html.tv

import play.api.mvc.RequestHeader

import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import views.html.base.layout.{ bits => layout }

import controllers.routes

object embed {

  private val dataStreamUrl = attr("data-stream-url")

  def apply(pov: lidraughts.game.Pov)(implicit config: lidraughts.app.ui.EmbedConfig) = frag(
    layout.doctype,
    layout.htmlTag(config.lang)(
      head(
        layout.charset,
        layout.viewport,
        layout.metaCsp(basicCsp(config.req)),
        st.headTitle("lidraughts.org draughts TV"),
        layout.pieceSprite(lidraughts.pref.PieceSet.default),
        responsiveCssTagWithTheme("tv.embed", config.bg)
      ),
      body(
        cls := s"base ${config.board}",
        dataStreamUrl := routes.Tv.feed
      )(
          div(id := "featured-game", cls := "embedded", title := "lidraughts.org TV")(
            gameFenNoCtx(pov, tv = true, blank = true),
            views.html.game.bits.vstext(pov)(none)
          ),
          jQueryTag,
          jsAt("javascripts/vendor/draughtsground.min.js", false),
          jsAt("compiled/tv.js", false)
        )
    )
  )
}
