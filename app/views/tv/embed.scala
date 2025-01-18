package views.html.tv

import controllers.routes

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._

object embed {

  import EmbedConfig.implicits.configLang

  private val dataStreamUrl = attr("data-stream-url")

  def apply(pov: lila.game.Pov)(implicit config: lila.app.ui.EmbedConfig) =
    views.html.base.embed(
      title = s"lishogi.org ${trans.shogi.txt()} TV",
      moreCss = cssTag("embed.tv"),
      moreJs = jsTag("embed.tv"),
      variant = pov.game.variant
    )(
      dataStreamUrl := routes.Tv.feed,
      div(id := "featured-game", cls := "embedded", title := "lishogi.org TV")(
        gameSfenNoCtx(pov, tv = true, blank = true),
        views.html.game.bits.vstext(pov)(config.lang)
      )
    )

}
