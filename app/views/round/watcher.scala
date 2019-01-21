package views.html
package round

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.HTTPRequest
import lidraughts.common.String.html.safeJsonValue
import lidraughts.game.Pov

import controllers.routes

object watcher {

  def apply(
    pov: Pov,
    data: play.api.libs.json.JsObject,
    tour: Option[lidraughts.tournament.TourMiniView],
    simul: Option[lidraughts.simul.Simul],
    cross: Option[lidraughts.game.Crosstable.WithMatchup],
    userTv: Option[lidraughts.user.User] = None,
    chatOption: Option[lidraughts.chat.UserChat.Mine],
    bookmarked: Boolean
  )(implicit ctx: Context) = {

    val chatJson = chatOption map { c =>
      chat.json(c.chat, name = trans.spectatorRoom.txt(), timeout = c.timeout, withNote = ctx.isAuth, public = true)
    }

    bits.layout(
      title = s"${gameVsText(pov.game, withRatings = true)} in ${pov.gameId}",
      side = game.side(pov, (data \ "game" \ "initialFen").asOpt[String].map(draughts.format.FEN), tour.map(_.tour), simul = simul, userTv = userTv, bookmarked = bookmarked),
      chat = chat.frag.some,
      underchat = Some(bits underchat pov.game),
      moreJs = frag(
        roundNvuiTag,
        roundTag,
        embedJs(s"""window.customWS = true; window.onload = function() {
LidraughtsRound.boot({ data: ${safeJsonValue(data)}, i18n: ${jsI18n(pov.game)}, chat: ${jsOrNull(chatJson)} }, document.getElementById('lidraughts'))}""")
      ),
      moreCss = cssTag("chat.css"),
      openGraph = povOpenGraph(pov).some,
      draughtsground = false
    ) {
        frag(
          div(cls := "round cg-512")(
            board.bits.domPreload(pov.some),
            bits.underboard(pov.game, cross)
          ),
          simul.map { s =>
            div(cls := "other_games", id := "now_playing")(
              h3()(simulStanding(s))
            )
          }
        )
      }
  }
}
