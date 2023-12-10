package views.html.tv

import lila.app.templating.Environment.*
import lila.app.ui.ScalatagsTemplate.*

object embed:

  private val dataStreamUrl = attr("data-stream-url") := "/tv/feed?bc=1"

  def apply(pov: lila.game.Pov)(using EmbedContext) =
    views.html.base.embed(
      title = "lichess.org chess TV",
      cssModule = "tv.embed"
    )(
      dataStreamUrl,
      div(id := "featured-game", cls := "embedded", title := "lichess.org TV")(
        views.html.game.mini.noCtx(pov, tv = true)(targetBlank)
      ),
      cashTag,
      chessgroundTag,
      jsModule("tvEmbed")
    )
