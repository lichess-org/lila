package views.html.tv

import lila.app.templating.Environment.*
import lila.app.ui.ScalatagsTemplate.*

object embed:

  private val defaultDataStreamUrl = "/tv/feed?bc=1"

  def apply(pov: lila.game.Pov, channelKey: Option[String] = None)(using EmbedContext) =
    val dataStreamUrl = channelKey.fold(defaultDataStreamUrl)(key => "/tv/" + key + "/feed?bc=1")
    views.html.base.embed(
      title = "lichess.org chess TV",
      cssModule = "tv.embed"
    )(
      attr("data-stream-url") := dataStreamUrl,
      div(id := "featured-game", cls := "embedded", title := "lichess.org TV")(
        views.html.game.mini.noCtx(pov, tv = true, channelKey)(targetBlank)
      ),
      cashTag,
      chessgroundTag,
      jsModule("tvEmbed")
    )
