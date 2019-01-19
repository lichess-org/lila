package views.html.tv

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
      title = s"${channel.name} TV: ${pov.fold(trans.noGameFound.txt())(p => s"${playerText(p.player)} vs ${playerText(p.opponent)}")}",
      side = side(channel, champions, "/tv", pov),
      underchat = Some(views.html.game.bits.watchers),
      moreJs = frag(
        roundTag,
        embedJs {
          def roundJs(p: lidraughts.game.Pov) = s"""LidraughtsRound.boot({ data: ${safeJsonValue(data)}, i18n: ${views.html.round.jsI18n(p.game)} }, document.getElementById('lidraughts'))"""
          s"""window.customWS = true;
window.onload = function() { ${pov ?? roundJs} }"""
        }
      ),
      moreCss = cssTag("tv.css"),
      draughtsground = false,
      openGraph = lidraughts.app.ui.OpenGraph(
        title = s"Watch the best ${channel.name.toLowerCase} games of lidraughts.org",
        description = s"Sit back, relax, and watch the best ${channel.name.toLowerCase} lidraughts players compete on lidraughts TV",
        url = s"$netBaseUrl${routes.Tv.onChannel(channel.key)}"
      ).some,
      robots = true
    ) {
        frag(
          div(cls := "round cg-512")(
            views.html.board.bits.domPreload(pov),
            div(cls := "underboard")(
              div(cls := "center")(
                cross map { c =>
                  div(cls := "crosstable")(
                    pov ?? { p => views.html.game.crosstable(ctx.userId.fold(c)(c.fromPov), p.gameId.some) }
                  )
                }
              )
            )
          ),
          div(cls := "game_list playing tv_history")(
            h2(trans.previouslyOnLidraughtsTV()),
            history.map { p =>
              div(views.html.game.bits.mini(p))
            }
          )
        )
      }
}
