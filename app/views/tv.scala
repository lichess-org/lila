package views.tv

import play.api.libs.json.*

import lila.app.UiEnv.{ *, given }
import lila.game.GameExt.perfType

val ui = lila.tv.ui.TvUi(helpers)(views.game.ui, views.tournament.ui.tournamentLink(_))

def index(
    channel: lila.tv.Tv.Channel,
    champions: lila.tv.Tv.Champions,
    pov: Pov,
    data: JsObject,
    cross: Option[lila.game.Crosstable.WithMatchup],
    history: List[Pov]
)(using Context) =
  val title    = s"${channel.name} TV: ${playerText(pov.player)} vs ${playerText(pov.opponent)}"
  val page     = views.round.ui.RoundPage(pov.game.variant, title)
  val roundApp = views.round.ui.roundAppPreload(pov)
  ui.index(channel, champions, pov, data, cross, history)(page)(roundApp)

def embed(pov: Pov, channelKey: Option[String])(using EmbedContext) =
  val dataStreamUrl = channelKey.fold("/tv/feed?bc=1")(key => s"/tv/${key}/feed?bc=1")
  views.base.embed.minimal(
    title = "lichess.org chess TV",
    cssKeys = List("bits.tv.embed"),
    modules = Esm("site.tvEmbed")
  )(
    attr("data-stream-url") := dataStreamUrl,
    div(id := "featured-game", cls := "embedded", title := "lichess.org TV")(
      views.game.mini.noCtx(pov, tv = true, channelKey)(targetBlank)
    ),
    cashTag,
    chessgroundTag
  )
