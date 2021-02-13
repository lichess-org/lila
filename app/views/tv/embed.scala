package views.html.tv

import controllers.routes

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._

object embed {

  private val dataStreamUrl = attr("data-stream-url")

  def apply(pov: lila.game.Pov)(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = "lichess.org chess TV",
      cssModule = "tv.embed"
    )(
      dataStreamUrl := routes.Tv.feed(),
      div(id := "featured-game", cls := "embedded", title := "lichess.org TV")(
        views.html.game.mini.noCtx(pov, tv = true, blank = true)
      ),
      jQueryTag,
      jsModule("tv.embed")
    )
}
