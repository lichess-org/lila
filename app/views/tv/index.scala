package views.html
package tv

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object index {

  def apply(
    channel: lila.tv.Tv.Channel,
    champions: lila.tv.Tv.Champions,
    pov: lila.game.Pov,
    data: play.api.libs.json.JsObject,
    cross: Option[lila.game.Crosstable.WithMatchup],
    flip: Boolean,
    history: List[lila.game.Pov]
  )(implicit ctx: Context) =
    views.html.round.bits.layout(
      variant = pov.game.variant,
      title = s"${channel.name} TV: ${playerText(pov.player)} vs ${playerText(pov.opponent)}",
      moreJs = frag(
        roundTag,
        embedJs {
          val transJs = views.html.round.jsI18n(pov.game)
          s"""lichess=window.lichess||{};customWS=true;
onload=function(){LichessRound.boot({data:${safeJsonValue(data)},i18n:$transJs})}"""
        }
      ),
      moreCss = responsiveCssTag("tv.single"),
      chessground = false,
      openGraph = lila.app.ui.OpenGraph(
        title = s"Watch the best ${channel.name.toLowerCase} games of lichess.org",
        description = s"Sit back, relax, and watch the best ${channel.name.toLowerCase} lichess players compete on lichess TV",
        url = s"$netBaseUrl${routes.Tv.onChannel(channel.key)}"
      ).some,
      robots = true
    )(
        main(cls := "round tv-single")(
          st.aside(cls := "round__side")(side(channel, champions, "/tv")),
          views.html.round.bits.roundAppPreload(pov, false),
          div(cls := "round__underboard")(
            views.html.round.bits.crosstable(cross, pov.game),
            div(cls := "tv-history")(
              h2(trans.previouslyOnLichessTV.frag()),
              div(cls := "now-playing")(
                history.map { p =>
                  a(href := gameLink(p))(views.html.game.bits.mini(p))
                }
              )
            )
          )
        )
      )
}
