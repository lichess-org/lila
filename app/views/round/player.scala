package views.round

import play.api.libs.json.Json

import lila.app.UiEnv.{ *, given }
import lila.common.Json.given
import lila.round.RoundGame.secondsSinceCreation

def player(
    pov: Pov,
    data: play.api.libs.json.JsObject,
    tour: Option[lila.tournament.GameView],
    simul: Option[lila.simul.Simul],
    cross: Option[lila.game.Crosstable.WithMatchup],
    playing: List[Pov],
    chatOption: Option[lila.chat.Chat.GameOrEvent],
    bookmarked: Boolean
)(using ctx: Context) =

  val chatJson = chatOption
    .map(_.either)
    .map:
      case Left(c) =>
        views.chat.restrictedJson(
          c,
          c.lines,
          name = trans.site.chatRoom.txt(),
          timeout = false,
          withNoteAge = ctx.isAuth.option(pov.game.secondsSinceCreation),
          public = false,
          resourceId = lila.chat.Chat.ResourceId(s"game/${c.chat.id}"),
          palantir = ctx.canPalantir
        )
      case Right((c, res)) =>
        views.chat.json(
          c.chat,
          c.lines,
          name = trans.site.chatRoom.txt(),
          timeout = c.timeout,
          public = true,
          resourceId = res
        )

  val opponentNameOrZen = if ctx.pref.isZen || ctx.pref.isZenAuto then "ZEN" else playerText(pov.opponent)
  ui.RoundPage(pov.game.variant, s"${trans.site.play.txt()} $opponentNameOrZen")
    .js(roundNvuiTag)
    .js(
      PageModule(
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
    )
    .graph(ui.povOpenGraph(pov))
    .zen
    .copy(playing = pov.game.playable):
      main(cls := "round")(
        st.aside(cls := "round__side")(
          side(pov, data, tour.map(_.tourAndTeamVs), simul, bookmarked = bookmarked),
          chatOption.map(_ => views.chat.frag)
        ),
        ui.roundAppPreload(pov),
        div(cls := "round__underboard")(
          crosstable(cross, pov.game),
          (playing.nonEmpty || simul.exists(_.isHost(ctx.me))).option(
            div(cls := "round__now-playing")(
              ui.others(playing, simul.filter(_.isHost(ctx.me)).map(views.simul.ui.roundOtherGames))
            )
          )
        ),
        div(cls := "round__underchat")(underchat(pov.game))
      )
