package views.html.analyse

import play.twirl.api.Html

import bits.dataPanel
import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue
import lidraughts.game.Pov

import controllers.routes

object replay {

  def apply(
    pov: Pov,
    data: play.api.libs.json.JsObject,
    initialFen: Option[draughts.format.FEN],
    pdn: String,
    analysis: Option[lidraughts.analyse.Analysis],
    analysisStarted: Boolean,
    simul: Option[lidraughts.simul.Simul],
    cross: Option[lidraughts.game.Crosstable.WithMatchup],
    userTv: Option[lidraughts.user.User],
    chatOption: Option[lidraughts.chat.UserChat.Mine],
    bookmarked: Boolean,
    onCheatList: Option[Boolean]
  )(implicit ctx: Context) = {

    import pov._

    val chatJson = chatOption map { c =>
      views.html.chat.json(c.chat, name = trans.spectatorRoom.txt(), timeout = c.timeout, withNote = ctx.isAuth, public = true)
    }

    bits.layout(
      title = s"${playerText(pov.game.whitePlayer)} vs ${playerText(pov.game.blackPlayer)}: ${game.opening.fold(trans.analysis.txt())(_.opening.ecoName)}",
      side = views.html.game.side(pov, initialFen, none, simul = simul, userTv = userTv, bookmarked = bookmarked),
      chat = views.html.chat.frag.some,
      underchat = Some(views.html.round.bits underchat pov.game),
      moreCss = cssTags("analyse.css", "chat.css"),
      moreJs = frag(
        analyseTag,
        analyseNvuiTag,
        embedJs(s"""lidraughts=lidraughts||{};
lidraughts.analyse={data:${safeJsonValue(data)},i18n:${jsI18n()},userId:$jsUserId,chat:${jsOrNull(chatJson)},
explorer:{endpoint:"$explorerEndpoint",tablebaseEndpoint:"$tablebaseEndpoint"}}""")
      ),
      openGraph = povOpenGraph(pov).some
    )(frag(
        div(cls := "analyse cg-512")(
          views.html.board.bits.domPreload(none)
        ),
        div(cls := "underboard_content none")(
          div(cls := "analysis_panels")(
            game.analysable option div(cls := "panel computer_analysis")(
              if (analysis.isDefined || analysisStarted) div(id := "adv_chart")
              else form(
                cls := s"future_game_analysis${ctx.isAnon ?? " must_login"}",
                action := routes.Analyse.requestAnalysis(gameId),
                method := "post"
              )(
                  button(`type` := "submit", cls := "button text")(
                    span(cls := "is3 text", dataIcon := "")(trans.requestAComputerAnalysis())
                  )
                )
            ),
            div(cls := "panel fen_pdn")(
              div(
                strong("FEN"),
                input(readonly := true, spellcheck := false, cls := "copyable autoselect fen")
              ),
              div(cls := "pdn_options")(
                strong("PDN"),
                div(
                  a(dataIcon := "x", cls := "text", rel := "nofollow", href := s"${routes.Game.exportOne(game.id)}?literate=1")(trans.downloadAnnotated()),
                  a(dataIcon := "x", cls := "text", rel := "nofollow", href := s"${routes.Game.exportOne(game.id)}?evals=0&clocks=0")(trans.downloadRaw()),
                  game.isPdnImport option a(dataIcon := "x", cls := "text", rel := "nofollow", href := s"${routes.Game.exportOne(game.id)}?imported=1")(trans.downloadImported()),
                  a(dataIcon := "=", cls := "text embed_howto", target := "_blank")(trans.embedInYourWebsite())
                )
              ),
              div(cls := "pdn")(pdn)
            ),
            div(cls := "panel move_times")(
              game.turns > 1 option div(id := "movetimes_chart")
            ),
            cross.map { c =>
              div(cls := "panel crosstable")(
                views.html.game.crosstable(pov.player.userId.fold(c)(c.fromPov), pov.gameId.some)
              )
            }
          ),
          div(cls := "analysis_menu")(
            game.analysable option
              a(
                dataPanel := "computer_analysis",
                cls := "computer_analysis",
                title := analysis.map { a => s"Provided by ${usernameOrId(a.providedBy)}" }
              )(trans.computerAnalysis()),
            !game.isPdnImport option frag(
              game.turns > 1 option a(dataPanel := "move_times", cls := "move_times")(trans.moveTimes()),
              cross.isDefined option a(dataPanel := "crosstable", cls := "crosstable")(trans.crosstable())
            ),
            a(dataPanel := "fen_pdn", cls := "fen_pdn")(raw("FEN &amp; PDN"))
          )
        )
      ))
  }
}
