package views.html.tv

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
      title = s"${channel.name} TV: ${playerText(pov.player)} vs ${playerText(pov.opponent)}",
      side = side(channel, champions, "/tv", pov.some),
      underchat = Some(views.html.game.bits.watchers),
      moreJs = frag(
        roundTag,
        embedJs {
          val transJs = views.html.round.jsI18n(pov.game)
          s"""window.customWS = true;
window.onload = function() { LichessRound.boot({ data: ${safeJsonValue(data)}, i18n: $transJs }, document.getElementById('lichess')) }"""
        }
      ),
      moreCss = cssTag("tv.css"),
      chessground = false,
      openGraph = lila.app.ui.OpenGraph(
        title = s"Watch the best ${channel.name.toLowerCase} games of lichess.org",
        description = s"Sit back, relax, and watch the best ${channel.name.toLowerCase} lichess players compete on lichess TV",
        url = s"$netBaseUrl${routes.Tv.onChannel(channel.key)}"
      ).some,
      robots = true
    ) {
        frag(
          div(cls := "round cg-512")(
            views.html.board.bits.domPreload(pov.some),
            div(cls := "underboard")(
              div(cls := "center")(
                cross map { c =>
                  views.html.game.crosstable(ctx.userId.fold(c)(c.fromPov), pov.gameId.some)
                }
              )
            )
          ),
          div(cls := "game_list playing tv_history")(
            h2(trans.previouslyOnLichessTV()),
            history.map { p =>
              div(views.html.game.bits.mini(p))
            }
          )
        )
      }
}
