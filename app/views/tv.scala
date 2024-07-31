package views.tv

import play.api.libs.json.*

import lila.app.UiEnv.{ *, given }
import lila.game.GameExt.perfType

def index(
    channel: lila.tv.Tv.Channel,
    champions: lila.tv.Tv.Champions,
    pov: Pov,
    data: JsObject,
    cross: Option[lila.game.Crosstable.WithMatchup],
    history: List[Pov]
)(using Context) =
  views.round.ui
    .RoundPage(
      pov.game.variant,
      s"${channel.name} TV: ${playerText(pov.player)} vs ${playerText(pov.opponent)}"
    )
    .js(PageModule("round", Json.obj("data" -> data, "i18n" -> views.round.jsI18n(pov.game))))
    .css("bits.tv.single")
    .graph(
      title = s"Watch the best ${channel.name.toLowerCase} games of lichess.org",
      description =
        s"Sit back, relax, and watch the best ${channel.name.toLowerCase} Lichess players compete on Lichess TV",
      url = s"$netBaseUrl${routes.Tv.onChannel(channel.key)}"
    )
    .zen
    .hrefLangs(lila.ui.LangPath(routes.Tv.index)):
      main(cls := "round tv-single")(
        st.aside(cls := "round__side")(
          side.meta(pov),
          side.channels(channel, champions, "/tv")
        ),
        views.round.ui.roundAppPreload(pov),
        div(cls := "round__underboard")(
          views.round.crosstable(cross, pov.game),
          div(cls := "tv-history")(
            h2(trans.site.previouslyOnLichessTV()),
            div(cls := "now-playing")(
              history.map { views.game.mini(_) }
            )
          )
        )
      )

def games(channel: lila.tv.Tv.Channel, povs: List[Pov], champions: lila.tv.Tv.Champions)(using ctx: Context) =
  Page(s"${channel.name} • ${trans.site.currentGames.txt()}")
    .css("bits.tv.games")
    .js(EsmInit("bits.tvGames")):
      main(
        cls     := "page-menu tv-games",
        dataRel := s"$netBaseUrl${routes.Tv.gameChannelReplacement(channel.key, GameId("gameId"), Nil)}"
      )(
        st.aside(cls := "page-menu__menu")(
          side.channels(channel, champions, "/games")
        ),
        div(cls := "page-menu__content now-playing")(
          povs.map { views.game.mini(_) }
        )
      )

def embed(pov: Pov, channelKey: Option[String])(using EmbedContext) =
  val dataStreamUrl = channelKey.fold("/tv/feed?bc=1")(key => s"/tv/${key}/feed?bc=1")
  views.base.embed.minimal(
    title = "lichess.org chess TV",
    cssKeys = List("bits.tv.embed"),
    modules = EsmInit("site.tvEmbed")
  )(
    attr("data-stream-url") := dataStreamUrl,
    div(id := "featured-game", cls := "embedded", title := "lichess.org TV")(
      views.game.mini.noCtx(pov, tv = true, channelKey)(targetBlank)
    ),
    cashTag,
    chessgroundTag
  )

object side:

  def channels(
      channel: lila.tv.Tv.Channel,
      champions: lila.tv.Tv.Champions,
      baseUrl: String
  ): Frag =
    lila.ui.bits.subnav(
      lila.tv.Tv.Channel.list.map: c =>
        a(
          href := s"$baseUrl/${c.key}",
          cls := List(
            "tv-channel" -> true,
            c.key        -> true,
            "active"     -> (c == channel)
          )
        ):
          span(dataIcon := c.icon):
            span(
              strong(c.name),
              span(cls := "champion")(
                champions
                  .get(c)
                  .fold[Frag](raw(" - ")): p =>
                    frag(
                      p.user.title.fold[Frag](p.user.name)(t => frag(t, nbsp, p.user.name)),
                      ratingTag(
                        " ",
                        p.rating
                      )
                    )
              )
            )
    )

  private val separator = " • "

  def meta(pov: Pov)(using Context): Frag =
    import pov.*
    div(cls := "game__meta")(
      st.section(
        div(cls := "game__meta__infos", dataIcon := views.game.ui.gameIcon(game))(
          div(cls := "header")(
            div(cls := "setup")(
              views.game.widgets.showClock(game),
              separator,
              (if game.rated then trans.site.rated else trans.site.casual).txt(),
              separator,
              variantLink(game.variant, game.perfType, shortName = true)
            )
          )
        ),
        div(cls := "game__meta__players"):
          game.players.mapList: p =>
            div(cls := s"player color-icon is ${p.color.name} text"):
              playerLink(p, withOnline = false, withDiff = true, withBerserk = true)
      ),
      game.tournamentId.map: tourId =>
        st.section(cls := "game__tournament-link"):
          a(href := routes.Tournament.show(tourId), dataIcon := Icon.Trophy, cls := "text"):
            views.tournament.ui.tournamentIdToName(tourId)
    )

  def sides(
      pov: Pov,
      cross: Option[lila.game.Crosstable.WithMatchup]
  )(using Context) =
    div(cls := "sides"):
      cross.map:
        views.game.ui.crosstable(_, pov.gameId.some)
