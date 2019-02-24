package views.html.analyse

import play.twirl.api.Html

import bits.dataPanel
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.game.Pov

import controllers.routes

object replay {

  def apply(
    pov: Pov,
    data: play.api.libs.json.JsObject,
    initialFen: Option[chess.format.FEN],
    pgn: String,
    analysis: Option[lila.analyse.Analysis],
    analysisStarted: Boolean,
    simul: Option[lila.simul.Simul],
    cross: Option[lila.game.Crosstable.WithMatchup],
    userTv: Option[lila.user.User],
    chatOption: Option[lila.chat.UserChat.Mine],
    bookmarked: Boolean
  )(implicit ctx: Context) = {

    import pov._

    val chatJson = chatOption map { c =>
      views.html.chat.json(c.chat, name = trans.spectatorRoom.txt(), timeout = c.timeout, withNote = ctx.isAuth, public = true)
    }
    val pgnLinks = div(
      a(dataIcon := "x", cls := "text", rel := "nofollow", href := s"${routes.Game.exportOne(game.id)}?literate=1")(trans.downloadAnnotated()),
      a(dataIcon := "x", cls := "text", rel := "nofollow", href := s"${routes.Game.exportOne(game.id)}?evals=0&clocks=0")(trans.downloadRaw()),
      game.isPgnImport option a(dataIcon := "x", cls := "text", rel := "nofollow", href := s"${routes.Game.exportOne(game.id)}?imported=1")(trans.downloadImported()),
      ctx.noBlind option a(dataIcon := "=", cls := "text embed-howto", target := "_blank")(trans.embedInYourWebsite())
    )

    bits.layout(
      title = s"${playerText(pov.game.whitePlayer)} vs ${playerText(pov.game.blackPlayer)}: ${game.opening.fold(trans.analysis.txt())(_.opening.ecoName)}",
      moreCss = responsiveCssTag("analyse"),
      moreJs = frag(
        analyseTag,
        analyseNvuiTag,
        embedJs(s"""lichess=lichess||{};
lichess.analyse={data:${safeJsonValue(data)},i18n:${jsI18n()},userId:$jsUserId,chat:${jsOrNull(chatJson)},
explorer:{endpoint:"$explorerEndpoint",tablebaseEndpoint:"$tablebaseEndpoint"}}""")
      ),
      openGraph = povOpenGraph(pov).some
    )(frag(
        main(cls := "analyse")(
          st.aside(cls := "analyse__side")(
            views.html.game.side(pov, initialFen, none, simul = simul, userTv = userTv, bookmarked = bookmarked)
          ),
          div(cls := "analyse__board main-board")(chessgroundSvg),
          div(cls := "analyse__tools"),
          div(cls := "analyse__controls")
        ),
        if (ctx.blind) div(cls := "blind_content none")(
          h2("PGN downloads"),
          pgnLinks
        )
        else frag(
          div(cls := "analyse__underboard none")(
            div(cls := "analyse__underboard__panels")(
              game.analysable option div(cls := "computer-analysis")(
                if (analysis.isDefined || analysisStarted) div(id := "adv-chart")
                else form(
                  cls := s"future-game-analysis${ctx.isAnon ?? " must-login"}",
                  action := routes.Analyse.requestAnalysis(gameId),
                  method := "post"
                )(
                    button(`type` := "submit", cls := "button text")(
                      span(cls := "is3 text", dataIcon := "")(trans.requestAComputerAnalysis())
                    )
                  )
              ),
              div(cls := "fen-pgn")(
                div(
                  strong("FEN"),
                  input(readonly := true, spellcheck := false, cls := "copyable autoselect analyse__underboard__fen")
                ),
                div(cls := "pgn-options")(
                  strong("PGN"),
                  pgnLinks
                ),
                div(cls := "pgn")(pgn)
              ),
              div(cls := "move-times")(
                game.turns > 1 option div(id := "movetimes-chart")
              ),
              cross.map { c =>
                div(cls := "ctable")(
                  views.html.game.crosstable(pov.player.userId.fold(c)(c.fromPov), pov.gameId.some)
                )
              }
            ),
            div(cls := "analyse__underboard__menu")(
              game.analysable option
                span(
                  dataPanel := "computer-analysis",
                  title := analysis.map { a => s"Provided by ${usernameOrId(a.providedBy)}" }
                )(trans.computerAnalysis()),
              !game.isPgnImport option frag(
                game.turns > 1 option span(dataPanel := "move-times")(trans.moveTimes()),
                cross.isDefined option span(dataPanel := "ctable")(trans.crosstable())
              ),
              span(dataPanel := "fen-pgn")(raw("FEN &amp; PGN"))
            )
          ),
          div(cls := "analyse__underchat none")(views.html.round.bits underchat pov.game)
        )
      ))
  }
}
