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

    bits.layout(
      title = s"${trans.play.txt()} ${if (ctx.pref.isZen) "ZEN" else playerText(pov.opponent)}",
      moreJs = frag(
        roundNvuiTag,
        roundTag,
        embedJs(s"""window.lidraughts=window.lidraughts||{};customWS=true;onload=function(){
LidraughtsRound.boot({data:${safeJsonValue(data)},i18n:${jsI18n(pov.game)},userId:$jsUserId,chat:${jsOrNull(chatJson)}
${tour.flatMap(_.top).??(top => s",tour:${safeJsonValue(lidraughts.tournament.JsonView.top(top, lightUser))}")}
})}""")
      ),
      moreCss = responsiveCssTag("round"),
      openGraph = povOpenGraph(pov).some,
      draughtsground = false,
      playing = true
    )(frag(
        main(cls := "round")(
          st.aside(cls := "round__side")(
            game.side(pov, (data \ "game" \ "initialFen").asOpt[String].map(draughts.format.FEN), tour.map(_.tour), simul, bookmarked = bookmarked)
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
              ))(others(pov, playing, simul))
          ),
          div(cls := "round__underchat")(bits underchat pov.game)
        )
      ))
  }
}
