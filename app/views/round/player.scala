package views.html
package round

import play.api.libs.json.Json

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue
import lila.game.Pov

object player:

  def apply(
      pov: Pov,
      data: play.api.libs.json.JsObject,
      tour: Option[lila.tournament.GameView],
      simul: Option[lila.simul.Simul],
      cross: Option[lila.game.Crosstable.WithMatchup],
      playing: List[Pov],
      chatOption: Option[lila.chat.Chat.GameOrEvent],
      bookmarked: Boolean
  )(using ctx: PageContext) =

    val chatJson = chatOption.map(_.either).map {
      case Left(c) =>
        chat.restrictedJson(
          c,
          name = trans.chatRoom.txt(),
          timeout = false,
          withNoteAge = ctx.isAuth option pov.game.secondsSinceCreation,
          public = false,
          resourceId = lila.chat.Chat.ResourceId(s"game/${c.chat.id}"),
          palantir = ctx.canPalantir
        )
      case Right((c, res)) =>
        chat.json(
          c.chat,
          name = trans.chatRoom.txt(),
          timeout = c.timeout,
          public = true,
          resourceId = res
        )
    }

    bits.layout(
      variant = pov.game.variant,
      title = s"${trans.play
          .txt()} ${if ctx.pref.isZen || ctx.pref.isZenAuto then "ZEN" else playerText(pov.opponent)}",
      moreJs = frag(
        roundNvuiTag,
        jsModuleInit(
          "round",
          Json
            .obj(
              "data"   -> data,
              "i18n"   -> jsI18n(pov.game),
              "userId" -> ctx.userId,
              "chat"   -> chatJson
            )
            .add("noab" -> ctx.me.exists(_.marks.engine))
        )
      ),
      openGraph = povOpenGraph(pov).some,
      playing = pov.game.playable,
      zenable = true
    ):
      main(cls := "round")(
        st.aside(cls := "round__side")(
          bits.side(pov, data, tour.map(_.tourAndTeamVs), simul, bookmarked = bookmarked),
          chatOption.map(_ => chat.frag)
        ),
        bits.roundAppPreload(pov),
        div(cls := "round__underboard")(
          bits.crosstable(cross, pov.game),
          (playing.nonEmpty || simul.exists(_ isHost ctx.me)) option
            div(
              cls := List(
                "round__now-playing" -> true,
                "blindfold"          -> ctx.pref.isBlindfold
              )
            )(bits.others(playing, simul.filter(_ isHost ctx.me)))
        ),
        div(cls := "round__underchat")(bits underchat pov.game)
      )
