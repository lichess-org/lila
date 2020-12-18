package views.html.tv

import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import views.html.base.layout.{ bits => layout }

import controllers.routes

object embed {

  private val dataStreamUrl = attr("data-stream-url")

  def apply(pov: lila.game.Pov)(implicit config: lila.app.ui.EmbedConfig) =
    frag(
      layout.doctype,
      layout.htmlTag(config.lang)(
        head(
          layout.charset,
          layout.viewport,
          layout.metaCsp(basicCsp(config.req)),
          st.headTitle("lishogi.org shogi TV"),
          layout.pieceSprite(lila.pref.PieceSet.default),
          cssTagWithTheme("tv.embed", config.bg)
        ),
        body(
          cls := s"base ${config.board}",
          dataStreamUrl := routes.Tv.feed()
        )(
          div(id := "featured-game", cls := "embedded", title := "lishogi.org TV")(
            gameFenNoCtx(pov, tv = true, blank = true),
            views.html.game.bits.vstext(pov)(none)
          ),
          jQueryTag,
          jsAt("javascripts/vendor/shogiground.min.js", false),
          jsAt("compiled/tv.js", false)
        )
      )
    )
}
