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
      title = s"${channel.name} TV: ${playerText(pov.player)} vs ${playerText(pov.opponent)}",
      moreJs = frag(
        roundTag,
        embedJsUnsafeLoadThen(
          s"""LichessRound.boot(${safeJsonValue(
            Json.obj(
              "data" -> data,
              "i18n" -> views.html.round.jsI18n(pov.game)
            )
          )})"""
        )
      ),
      moreCss = cssTag("tv.single"),
      chessground = false,
      openGraph = lila.app.ui
        .OpenGraph(
          title = s"Watch the best ${channel.name.toLowerCase} games of lichess.org",
          description =
            s"Sit back, relax, and watch the best ${channel.name.toLowerCase} Lichess players compete on Lichess TV",
          url = s"$netBaseUrl${routes.Tv.onChannel(channel.key)}"
        )
        .some,
      robots = true
    )(
      main(cls := "round tv-single")(
        st.aside(cls := "round__side")(
          side.meta(pov),
          side.channels(channel, champions, "/tv")
        ),
        views.html.round.bits.roundAppPreload(pov, controls = false),
        div(cls := "round__underboard")(
          views.html.round.bits.crosstable(cross, pov.game),
          div(cls := "tv-history")(
            h2(trans.previouslyOnLichessTV()),
            div(cls := "now-playing")(
              history map { views.html.game.mini(_) }
            )
          )
        )
      )
    )
}
