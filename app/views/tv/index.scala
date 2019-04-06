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
          s"""window.lichess=window.lichess||{};window.customWS=true;
window.onload=function(){LichessRound.boot({data:${safeJsonValue(data)},i18n:$transJs})}"""
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
    )(frag(
        main(cls := "round tv-single")(
          st.aside(cls := "round__side")(
            side(channel, champions, "/tv", pov.some)
          ),
          div(cls := "round__board main-board")(board.bits.domPreload(pov.some))
        ),
        div(cls := "round__underboard none")(
          round.bits.crosstable(cross, pov.game),
          div(cls := "now-playing tv-history")(
            h2(trans.previouslyOnLichessTV()),
            history.map { p =>
              div(views.html.game.bits.mini(p))
            }
          )
        ),
        div(cls := "round__underchat none")(views.html.game.bits.watchers)
      ))
}
