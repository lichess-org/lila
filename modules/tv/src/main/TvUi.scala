package lila.tv
package ui

import play.api.libs.json.{ Json, JsObject }

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.game.ui.GameUi
import lila.core.i18n.Translate

final class TvUi(helpers: lila.ui.Helpers)(
    gameUi: GameUi,
    tournamentLink: TourId => Translate ?=> Tag
):
  import helpers.{ *, given }

  extension (channel: Tv.Channel)
    private def translate(using Context): String =
      (channel.speed.map(_.key) orElse channel.variant.map(_.key))
        .flatMap(k => PerfKey(k.toString))
        .map(_.perfTrans)
        .getOrElse {
          channel match
            case Tv.Channel.Computer => trans.site.computer
            case _ => channel.name
        }

  def index(
      channel: Tv.Channel,
      champions: Tv.Champions,
      pov: Pov,
      data: JsObject,
      cross: Option[lila.game.Crosstable.WithMatchup],
      history: List[Pov]
  )(page: Page)(roundApp: Tag)(using Context) =
    page
      .js(PageModule("round", Json.obj("data" -> data)))
      .css("bits.tv.single")
      .graph(
        title = s"Watch the best ${channel.name.toLowerCase} games of lichess.org",
        description =
          s"Sit back, relax, and watch the best ${channel.name.toLowerCase} Lichess players compete on Lichess TV",
        url = routeUrl(routes.Tv.onChannel(channel.key))
      )
      .flag(_.zen)
      .hrefLangs(lila.ui.LangPath(routes.Tv.index)):
        main(cls := "round tv-single")(
          st.aside(cls := "round__side")(
            side.meta(pov),
            side.channels(channel, champions, "/tv")
          ),
          roundApp,
          div(cls := "round__underboard")(
            gameUi.crosstable.option(cross, pov.game),
            div(cls := "tv-history")(
              h2(trans.site.previouslyOnLichessTV()),
              div(cls := "now-playing"):
                history.map { gameUi.mini(_) }
            )
          )
        )

  def games(channel: Tv.Channel, povs: List[Pov], champions: Tv.Champions)(using Context) =
    Page(s"${channel.translate} • ${trans.site.currentGames.txt()}")
      .css("bits.tv.games")
      .js(Esm("bits.tvGames")):
        main(
          cls := "page-menu tv-games",
          dataRel := routeUrl(routes.Tv.gameChannelReplacement(channel.key, GameId("gameId"), Nil))
        )(
          st.aside(cls := "page-menu__menu"):
            side.channels(channel, champions, "/games")
          ,
          div(cls := "page-menu__content now-playing"):
            povs.map(gameUi.mini(_))
        )

  object side:

    private val separator = " • "

    def meta(pov: Pov)(using Context): Frag =
      import pov.*
      div(cls := "game__meta")(
        st.section(
          div(cls := "game__meta__infos", dataIcon := gameUi.gameIcon(game))(
            div(cls := "header")(
              div(cls := "setup")(
                gameUi.widgets.showClock(game),
                separator,
                ratedName(game.rated),
                separator,
                variantLink(game.variant, game.perfKey, shortName = true)
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
            tournamentLink(tourId)
      )

    def channels(channel: Tv.Channel, champions: Tv.Champions, baseUrl: String)(using Context): Frag =
      lila.ui.bits.subnav:
        Tv.Channel.list.map: c =>
          a(
            href := s"$baseUrl/${c.key}",
            cls := List(
              "tv-channel" -> true,
              c.key -> true,
              "active" -> (c == channel)
            )
          ):
            span(dataIcon := c.icon):
              span(
                strong(c.translate),
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

    def sides(
        pov: Pov,
        cross: Option[lila.game.Crosstable.WithMatchup]
    )(using Context) =
      div(cls := "sides"):
        cross.map:
          gameUi.crosstable(_, pov.gameId.some)
