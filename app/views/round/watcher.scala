package views.round

import play.api.libs.json.{ JsObject, Json }

import lila.app.UiEnv.{ *, given }
import lila.round.RoundGame.secondsSinceCreation

def watcher(
    pov: Pov,
    data: JsObject,
    tour: Option[lila.tournament.TourAndTeamVs],
    simul: Option[lila.simul.Simul],
    cross: Option[lila.game.Crosstable.WithMatchup],
    userTv: Option[User] = None,
    chatOption: Option[lila.chat.UserChat.Mine],
    bookmarked: Boolean
)(using ctx: Context) =

  val chatJson = chatOption.map: c =>
    views.chat.json(
      c.chat,
      c.lines,
      name = trans.site.spectatorRoom.txt(),
      timeout = c.timeout,
      withNoteAge = ctx.isAuth.option(pov.game.secondsSinceCreation),
      public = true,
      resource = lila.core.chat.PublicSource.Watcher(pov.gameId),
      voiceChat = ctx.canVoiceChat,
      opponentId = pov.opponent.userId
    )

  ui.RoundPage(pov.game.variant, s"${gameVsText(pov.game, withRatings = ctx.pref.showRatings)} • spectator")
    .js(roundNvuiTag)
    .js(
      PageModule(
        "round",
        Json.obj(
          "data" -> data,
          "chat" -> chatJson
        )
      )
    )
    .graph(ui.povOpenGraph(pov))
    .flag(_.zen):
      main(cls := "round")(
        st.aside(cls := "round__side")(
          side(pov, data, tour, simul, userTv, bookmarked),
          chatOption.map(_ => views.chat.frag)
        ),
        ui.roundAppPreload(pov),
        div(cls := "round__underboard")(views.game.ui.crosstable.option(cross, pov.game)),
        div(cls := "round__underchat")(underchat(pov.game))
      )

def crawler(pov: Pov, initialFen: Option[chess.format.Fen.Full], pgn: chess.format.pgn.Pgn)(using
    ctx: Context
) =
  ui.RoundPage(pov.game.variant, gameVsText(pov.game, withRatings = true))
    .graph(ui.povOpenGraph(pov)):
      main(cls := "round")(
        st.aside(cls := "round__side")(
          views.game.side(pov, initialFen, none, simul = none, userTv = none, bookmarked = false),
          div(cls := "for-crawler")(
            h1(titleGame(pov.game)),
            p(ui.describePov(pov)),
            div(cls := "pgn")(pgn.render)
          )
        ),
        div(cls := "round__board main-board")(ui.povChessground(pov))
      )
