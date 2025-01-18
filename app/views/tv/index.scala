package views.html
package tv

import controllers.routes
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

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
        moduleJsTag(
          "round",
          Json.obj(
            "data" -> data
          )
        )
      ),
      moreCss = cssTag("misc.tv.single"),
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
