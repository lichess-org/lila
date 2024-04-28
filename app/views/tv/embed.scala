package views.tv

import lila.app.templating.Environment.{ *, given }

object embed:

  private val defaultDataStreamUrl = "/tv/feed?bc=1"

  def apply(pov: Pov, channelKey: Option[String])(using EmbedContext) =
    val dataStreamUrl = channelKey.fold(defaultDataStreamUrl)(key => s"/tv/${key}/feed?bc=1")
    views.base.embed(
      title = "lichess.org chess TV",
      cssModule = "tv.embed",
      modules = EsmInit("site.tvEmbed")
    )(
      attr("data-stream-url") := dataStreamUrl,
      div(id := "featured-game", cls := "embedded", title := "lichess.org TV")(
        views.game.mini.noCtx(pov, tv = true, channelKey)(targetBlank)
      ),
      cashTag,
      chessgroundTag
    )
