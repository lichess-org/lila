package views.html
package tv

import play.api.libs.json.Json

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
      history: List[lila.game.Pov]
  )(implicit ctx: Context) =
    views.html.round.bits.layout(
      variant = pov.game.variant,
      title = s"${transKeyTxt(channel.key)} TV: ${playerText(pov.player)} vs ${playerText(pov.opponent)}",
      moreJs = frag(
        roundTag,
        embedJsUnsafe(
          s"""lishogi=window.lishogi||{};customWS=true;onload=function(){LishogiRound.boot(${safeJsonValue(
              Json.obj(
                "data" -> data,
                "i18n" -> views.html.round.jsI18n(pov.game)
              )
            )})}"""
        )
      ),
      moreCss = cssTag("tv.single"),
      shogiground = false,
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"${trans.watchLishogiTV.txt()} - ${transKeyTxt(channel.key)}",
          description = trans.watchLishogiTVDescription.txt(),
          url = s"$netBaseUrl${routes.Tv.onChannel(channel.key)}"
        )
        .some,
      robots = true,
      withHrefLangs = lila.i18n.LangList.All.some
    )(
      main(cls := s"round tv-single ${mainVariantClass(pov.game.variant)}")(
        st.aside(cls := "round__side")(
          side.meta(pov),
          side.channels(channel, champions, "/tv")
        ),
        views.html.round.bits.roundAppPreload(pov, false),
        div(cls := "round__underboard")(
          views.html.round.bits.crosstable(cross, pov.game),
          div(cls := "tv-history")(
            h2(trans.previouslyOnLishogiTV()),
            div(cls := "now-playing")(
              history map views.html.game.bits.mini
            )
          )
        )
      )
    )
}
