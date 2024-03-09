package views.html
package round

import play.api.libs.json.{ JsObject, Json }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.game.Pov

object watcher {

  def apply(
      pov: Pov,
      data: JsObject,
      tour: Option[lila.tournament.TourAndTeamVs],
      simul: Option[lila.simul.Simul],
      cross: Option[lila.game.Crosstable.WithMatchup],
      userTv: Option[lila.user.User] = None,
      chatOption: Option[lila.chat.UserChat.Mine],
      bookmarked: Boolean
  )(implicit ctx: Context) = {

    val chatJson = chatOption map { c =>
      chat.json(
        c.chat,
        name = trans.spectatorRoom.txt(),
        timeout = c.timeout,
        withNoteAge = ctx.isAuth option pov.game.secondsSinceCreation,
        public = true,
        resourceId = lila.chat.Chat.ResourceId(s"game/${c.chat.id}"),
        palantir = ctx.me.exists(_.canPalantir)
      )
    }

    bits.layout(
      variant = pov.game.variant,
      title = gameVsText(pov.game, withRatings = true),
      moreJs = frag(
        roundNvuiTag,
        roundTag,
        embedJsUnsafe(s"""lishogi=window.lishogi||{};customWS=true;onload=function(){
LishogiRound.boot(${safeJsonValue(
            Json.obj(
              "data" -> data,
              "i18n" -> jsI18n(pov.game),
              "chat" -> chatJson
            )
          )})}""")
      ),
      openGraph = povOpenGraph(pov).some,
      shogiground = false
    )(
      main(cls := s"round ${mainVariantClass(pov.game.variant)}")(
        st.aside(cls := "round__side")(
          bits.side(pov, tour, simul, userTv, bookmarked),
          chatOption.map(_ => chat.frag)
        ),
        bits.roundAppPreload(pov, false),
        div(cls := "round__underboard")(bits.crosstable(cross, pov.game)),
        div(cls := "round__underchat")(bits underchat pov.game)
      )
    )
  }

  def crawler(pov: Pov, kif: shogi.format.Notation)(implicit
      ctx: Context
  ) =
    bits.layout(
      variant = pov.game.variant,
      title = gameVsText(pov.game, withRatings = true),
      openGraph = povOpenGraph(pov).some,
      shogiground = false
    )(
      frag(
        main(cls := s"round ${mainVariantClass(pov.game.variant)}")(
          st.aside(cls := "round__side")(
            game.side(pov, none, simul = none, userTv = none, bookmarked = false),
            div(cls := "for-crawler")(
              h1(titleGame(pov.game)),
              p(describePov(pov)),
              div(cls := "kif")(kif.render)
            )
          ),
          div(cls := s"round__board main-board ${variantClass(pov.game.variant)}")(shogiground(pov))
        )
      )
    )
}
