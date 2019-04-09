package views.html.tv

import play.api.mvc.RequestHeader

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import views.html.base.layout.bits

import controllers.routes

object embed {

  private val dataStreamUrl = attr("data-stream-url")

  def apply(pov: lidraughts.game.Pov, bg: String, board: String)(implicit req: RequestHeader) = frag(
    bits.doctype,
    html(
      head(
        bits.charset,
        bits.metaCsp(basicCsp),
        st.headTitle("lidraughts.org draughts TV"),
        bits.pieceSprite(lidraughts.pref.PieceSet.default),
        responsiveCssTagWithTheme("tv.embed", bg)
      ),
      body(
        cls := s"base $board wide_crown",
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
