package views.html.tv

import controllers.routes

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._

object embed {

  private val dataStreamUrl = attr("data-stream-url") := "/tv/feed?bc=1"

  def apply(pov: lila.game.Pov)(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = "lichess.org chess TV",
      cssModule = "tv.embed"
    )(
      dataStreamUrl,
      div(id := "featured-game", cls := "embedded", title := "lichess.org TV")(
        views.html.game.mini.noCtx(pov, tv = true)(targetBlank)
      ),
      cashTag,
      jsModule("tvEmbed")
    )
}
