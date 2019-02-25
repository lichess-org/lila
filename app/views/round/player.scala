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
      moreJs = frag(
        roundNvuiTag,
        roundTag,
        embedJs(s"""window.lichess=window.lichess||{};customWS=true;onload=function(){
LichessRound.boot({data:${safeJsonValue(data)},i18n:${jsI18n(pov.game)},userId:$jsUserId,chat:${jsOrNull(chatJson)}
${tour.flatMap(_.top).??(top => s",tour:${safeJsonValue(lila.tournament.JsonView.top(top, lightUser))}")}
})}""")
      ),
      moreCss = responsiveCssTag("round"),
      openGraph = povOpenGraph(pov).some,
      chessground = false,
      playing = true
    )(frag(
        main(cls := "round")(
          st.aside(cls := "round__side")(
            game.side(pov, (data \ "game" \ "initialFen").asOpt[String].map(chess.format.FEN), tour.map(_.tour), simul, bookmarked = bookmarked)
          ),
          chatOption.map(_ => chat.frag),
          div(cls := "round__app")(
            div(cls := "round__board main-board")(board.bits.domPreload(pov.some))
          ),
          div(cls := "round__underboard")(
            bits.crosstable(cross, pov.game),
            (playing.nonEmpty || simul.nonEmpty) option
              div(cls := List(
                "round__now-playing" -> true,
                "other_games" -> true,
                "blindfold" -> ctx.pref.isBlindfold
              ))(others(playing, simul))
          ),
          div(cls := "round__underchat")(bits underchat pov.game)
        )
      ))
  }
}
