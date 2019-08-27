package views.html
package round

import play.api.libs.json.Json

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
      case Left(c) => chat.restrictedJson(
        c,
        name = trans.chatRoom.txt(),
        timeout = false,
        withNote = ctx.isAuth,
        public = false,
        resourceId = lidraughts.chat.Chat.ResourceId(s"game/${c.chat.id}"),
        palantir = ctx.me.exists(_.canPalantir)
      )
      case Right((c, res)) => chat.json(
        c.chat,
        name = trans.chatRoom.txt(),
        timeout = c.timeout,
        public = true,
        resourceId = res
      )
    }

    bits.layout(
      variant = pov.game.variant,
      title = s"${trans.play.txt()} ${if (ctx.pref.isZen) "ZEN" else playerText(pov.opponent)}",
      moreJs = frag(
        roundNvuiTag,
        roundTag,
        embedJsUnsafe(s"""lidraughts=window.lidraughts||{};customWS=true;onload=function(){
LidraughtsRound.boot(${
          safeJsonValue(Json.obj(
            "data" -> data,
            "i18n" -> jsI18n(pov.game),
            "userId" -> ctx.userId,
            "chat" -> chatJson
          ) ++ tour.flatMap(_.top).??(top => Json.obj(
              "tour" -> lidraughts.tournament.JsonView.top(top, lightUser)
            )))
        })}""")
      ),
      openGraph = povOpenGraph(pov).some,
      draughtsground = false,
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
            (playing.nonEmpty || simul.nonEmpty) option
              div(cls := List(
                "round__now-playing" -> true,
                "blindfold" -> ctx.pref.isBlindfold
              ))(bits.others(pov, playing, simul))
          ),
          div(cls := "round__underchat")(bits underchat pov.game)
        )
      )
  }
}
