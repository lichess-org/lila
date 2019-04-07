package views.html
package tv

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object index {

  def apply(
    channel: lidraughts.tv.Tv.Channel,
    champions: lidraughts.tv.Tv.Champions,
    pov: Option[lidraughts.game.Pov],
    data: play.api.libs.json.JsObject,
    cross: Option[lidraughts.game.Crosstable.WithMatchup],
    flip: Boolean,
    history: List[lidraughts.game.Pov]
  )(implicit ctx: Context) =
    views.html.round.bits.layout(
      variant = pov.fold[draughts.variant.Variant](draughts.variant.Standard)(_.game.variant),
      title = s"${channel.name} TV: ${pov.fold(trans.noGameFound.txt())(p => s"${playerText(p.player)} vs ${playerText(p.opponent)}")}",
      moreJs = frag(
        roundTag,
        embedJs {
          val transJs = pov.map { p => views.html.round.jsI18n(p.game) }
          s"""window.lidraughts=window.lidraughts||{};customWS=true;
onload=function(){LidraughtsRound.boot({data:${safeJsonValue(data)},i18n:$transJs})}"""
        }
      ),
      moreCss = responsiveCssTag("tv.single"),
      draughtsground = false,
      openGraph = lidraughts.app.ui.OpenGraph(
        title = s"Watch the best ${channel.name.toLowerCase} games of lidraughts.org",
        description = s"Sit back, relax, and watch the best ${channel.name.toLowerCase} lidraughts players compete on lidraughts TV",
        url = s"$netBaseUrl${routes.Tv.onChannel(channel.key)}"
      ).some,
      robots = true
    )(
        main(cls := "round tv-single")(
          st.aside(cls := "round__side")(side(channel, champions, "/tv")),
          pov ?? { pv =>
            frag(
              views.html.round.bits.roundAppPreload(pv, false),
              div(cls := "round__underboard")(
                views.html.round.bits.crosstable(cross, pv.game),
                div(cls := "now-playing tv-history")(
                  h2(trans.previouslyOnLidraughtsTV.frag()),
                  history.map { p =>
                    div(views.html.game.bits.mini(p))
                  }
                )
              )
            )
          }
        )
      )
}
