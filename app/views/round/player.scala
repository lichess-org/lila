package views.html
package round

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.HTTPRequest
import lidraughts.common.String.html.safeJsonValue
import lidraughts.game.Pov

import controllers.routes

object player {

  def apply(
    pov: Pov,
    data: play.api.libs.json.JsObject,
    tour: Option[lidraughts.tournament.TourMiniView],
    simul: Option[lidraughts.simul.Simul],
    cross: Option[lidraughts.game.Crosstable.WithMatchup],
    playing: List[Pov],
    chatOption: Option[lidraughts.chat.Chat.GameOrEvent],
    bookmarked: Boolean
  )(implicit ctx: Context) = {

    val chatJson = chatOption.map(_.either).map {
      case Left(c) => chat.restrictedJson(c, name = trans.chatRoom.txt(), timeout = false, withNote = ctx.isAuth, public = false)
      case Right(c) => chat.json(c.chat, name = trans.chatRoom.txt(), timeout = c.timeout, public = true)
    }

    layout(
      title = s"${trans.play.txt()} ${if (ctx.pref.isZen) "ZEN" else playerText(pov.opponent)} in ${pov.gameId}",
      side = game.side(pov, (data \ "game" \ "initialFen").asOpt[String].map(draughts.format.FEN), tour.map(_.tour), simul, bookmarked = bookmarked),
      chat = chatOption.map(_ => chat.html),
      underchat = Some(bits underchat pov.game),
      moreJs = frag(
        roundTag,
        embedJs(s"""window.customWS = true; window.onload = function() {
LidraughtsRound.boot({ data: ${safeJsonValue(data)}, i18n: ${jsI18n(pov.game)}, userId: $jsUserId, chat: ${jsOrNull(chatJson)},
${tour.??(t => s"tour: ${toJson(tour.flatMap(_.top).map(lidraughts.tournament.JsonView.top(_, lightUser)))}")}
}, document.getElementById('lidraughts'))}""")
      ),
      moreCss = cssTag("chat.css"),
      openGraph = povOpenGraph(pov).some,
      draughtsground = false,
      playing = true
    ) {
        frag(
          div(cls := "round cg-512")(
            board.bits.domPreload(pov.some),
            bits.underboard(pov.game, cross)
          ),
          (playing.nonEmpty || simul.nonEmpty) option
            div(id := "now_playing", cls := List("other_games" -> true, "blindfold" -> ctx.pref.isBlindfold))(
              others(pov, playing, simul)
            )
        )
      }
  }
}
