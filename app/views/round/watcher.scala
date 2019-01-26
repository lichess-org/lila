package views.html
package round

import lila.api.Context
import lila.app.templating.Environment._
import lila.common.HTTPRequest
import lila.common.String.html.safeJsonValue
import lila.game.Pov
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object watcher {

  def apply(
    pov: Pov,
    data: play.api.libs.json.JsObject,
    tour: Option[lila.tournament.TourMiniView],
    simul: Option[lila.simul.Simul],
    cross: Option[lila.game.Crosstable.WithMatchup],
    userTv: Option[lila.user.User] = None,
    chatOption: Option[lila.chat.UserChat.Mine],
    bookmarked: Boolean
  )(implicit ctx: Context) = {

    val chatJson = chatOption map { c =>
      chat.json(c.chat, name = trans.spectatorRoom.txt(), timeout = c.timeout, withNote = ctx.isAuth, public = true)
    }

    bits.layout(
      title = gameVsText(pov.game, withRatings = true),
      side = game.side(pov, (data \ "game" \ "initialFen").asOpt[String].map(chess.format.FEN), tour.map(_.tour), simul = simul, userTv = userTv, bookmarked = bookmarked),
      chat = chat.frag.some,
      underchat = Some(bits underchat pov.game),
      moreJs = frag(
        roundNvuiTag,
        roundTag,
        embedJs(s"""window.customWS = true; window.onload = function() {
LichessRound.boot({ data: ${safeJsonValue(data)}, i18n: ${jsI18n(pov.game)}, chat: ${jsOrNull(chatJson)} }, document.getElementById('lichess'))}""")
      ),
      moreCss = cssTag("chat.css"),
      openGraph = povOpenGraph(pov).some,
      chessground = false
    ) {
        div(cls := "round cg-512")(
          board.bits.domPreload(pov.some),
          bits.underboard(pov.game, cross)
        )
      }
  }
}
