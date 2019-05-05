package views.html
package round

import play.api.libs.json.Json

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
      variant = pov.game.variant,
      title = s"${trans.play.txt()} ${if (ctx.pref.isZen) "ZEN" else playerText(pov.opponent)}",
      moreJs = frag(
        roundNvuiTag,
        roundTag,
        embedJsUnsafe(s"""lichess=window.lichess||{};customWS=true;onload=function(){
LichessRound.boot(${
          safeJsonValue(Json.obj(
            "data" -> data,
            "i18n" -> jsI18n(pov.game),
            "userId" -> ctx.userId,
            "chat" -> chatJson
          ) ++ tour.flatMap(_.top).??(top => Json.obj(
              "tour" -> lila.tournament.JsonView.top(top, lightUser)
            )))
        })}""")
      ),
      openGraph = povOpenGraph(pov).some,
      chessground = false,
      playing = true
    )(
        main(cls := "round")(
          st.aside(cls := "round__side")(
            bits.side(pov, data, tour, simul, bookmarked = bookmarked),
            chatOption.map(_ => chat.frag)
          ),
          bits.roundAppPreload(pov, true),
          div(cls := "round__underboard")(
            bits.crosstable(cross, pov.game),
            (playing.nonEmpty || simul.exists(_ isHost ctx.me)) option
              div(cls := List(
                "round__now-playing" -> true,
                "blindfold" -> ctx.pref.isBlindfold
              ))(bits.others(playing, simul))
          ),
          div(cls := "round__underchat")(bits underchat pov.game)
        )
      )
  }
}
