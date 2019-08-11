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
      chat.json(
        c.chat,
        name = trans.spectatorRoom.txt(),
        timeout = c.timeout,
        withNote = ctx.isAuth,
        public = true,
        palantir = ctx.me.exists(_.canPalantir)
      )
    }

    bits.layout(
      variant = pov.game.variant,
      title = gameVsText(pov.game, withRatings = true),
      moreJs = frag(
        roundNvuiTag,
        roundTag,
        embedJsUnsafe(s"""lidraughts=window.lidraughts||{};customWS=true;onload=function(){
LidraughtsRound.boot(${
          safeJsonValue(Json.obj(
            "data" -> data,
            "i18n" -> jsI18n(pov.game),
            "chat" -> chatJson
          ))
        })}""")
      ),
      openGraph = povOpenGraph(pov).some,
      draughtsground = false
    )(
        main(cls := "round")(
          st.aside(cls := "round__side")(
            bits.side(pov, data, tour, simul, userTv, bookmarked),
            chatOption.map(_ => chat.frag)
          ),
          bits.roundAppPreload(pov, false),
          div(cls := "round__underboard")(
            bits.crosstable(cross, pov.game),
            simul.map { s =>
              div(cls := List(
                "round__now-playing" -> true,
                "blindfold" -> ctx.pref.isBlindfold
              ))(
                h3(bits.simulStanding(s)),
                h3(bits.simulTarget(s))
              )
            }
          ),
          div(cls := "round__underchat")(bits underchat pov.game)
        )
      )
  }

  def crawler(pov: Pov, initialFen: Option[draughts.format.FEN], pdn: draughts.format.pdn.Pdn)(implicit ctx: Context) =
    bits.layout(
      variant = pov.game.variant,
      title = gameVsText(pov.game, withRatings = true),
      openGraph = povOpenGraph(pov).some,
      draughtsground = false
    )(frag(
        main(cls := "round")(
          st.aside(cls := "round__side")(
            game.side(pov, initialFen, none, simul = none, userTv = none, bookmarked = false),
            div(cls := "for-crawler")(
              h1(titleGame(pov.game)),
              p(describePov(pov)),
              div(cls := "pdn")(pdn.render)
            )
          ),
          div(cls := "round__board main-board")(draughtsground(pov))
        )
      ))
}
