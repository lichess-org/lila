package views.html
package round

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.HTTPRequest
import lila.common.String.html.safeJsonValue
import lila.game.Pov

import controllers.routes

object player {

  def apply(
    pov: Pov,
    data: play.api.libs.json.JsObject,
    tour: Option[lila.tournament.TourMiniView],
    simul: Option[lila.simul.Simul],
    cross: Option[lila.game.Crosstable.WithMatchup],
    playing: List[Pov],
    chatOption: Option[lila.chat.Chat.GameOrEvent],
    bookmarked: Boolean
  )(implicit ctx: Context) = {

    val chatJson = chatOption.map(_.either).map {
      case Left(c) => chat.restrictedJson(c, name = trans.chatRoom.txt(), timeout = false, withNote = ctx.isAuth, public = false)
      case Right(c) => chat.json(c.chat, name = trans.chatRoom.txt(), timeout = c.timeout, public = true)
    }

    bits.layout(
      title = s"${trans.play.txt()} ${if (ctx.pref.isZen) "ZEN" else playerText(pov.opponent)}",
      side = game.side(pov, (data \ "game" \ "initialFen").asOpt[String].map(chess.format.FEN), tour.map(_.tour), simul, bookmarked = bookmarked),
      chat = chatOption.map(_ => chat.frag),
      underchat = Some(bits underchat pov.game),
      moreJs = frag(
        roundNvuiTag,
        roundTag,
        embedJs(s"""window.customWS=true;window.onload=function(){
LichessRound.boot({data:${safeJsonValue(data)},i18n:${jsI18n(pov.game)},userId:$jsUserId,chat:${jsOrNull(chatJson)}
${tour.flatMap(_.top).??(top => s",tour:${safeJsonValue(lila.tournament.JsonView.top(top, lightUser))}")}
}, document.getElementById('lichess'))}""")
      ),
      moreCss = cssTag("chat.css"),
      openGraph = povOpenGraph(pov).some,
      chessground = false,
      playing = true
    ) {
        frag(
          div(cls := "round cg-512")(
            board.bits.domPreload(pov.some),
            bits.underboard(pov.game, cross)
          ),
          (playing.nonEmpty || simul.nonEmpty) option
            div(id := "now_playing", cls := List("other_games" -> true, "blindfold" -> ctx.pref.isBlindfold))(
              others(playing, simul)
            )
        )
      }
  }
}
