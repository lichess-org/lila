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
    val pdnLinks = div(
      a(dataIcon := "x", cls := "text", rel := "nofollow", href := s"${routes.Game.exportOne(game.id)}?literate=1")(trans.downloadAnnotated()),
      a(dataIcon := "x", cls := "text", rel := "nofollow", href := s"${routes.Game.exportOne(game.id)}?evals=0&clocks=0")(trans.downloadRaw()),
      game.isPdnImport option a(dataIcon := "x", cls := "text", rel := "nofollow", href := s"${routes.Game.exportOne(game.id)}?imported=1")(trans.downloadImported()),
      ctx.noBlind option a(dataIcon := "=", cls := "text embed-howto", target := "_blank")(trans.embedInYourWebsite())
    )

    bits.layout(
      title = s"${playerText(pov.game.whitePlayer)} vs ${playerText(pov.game.blackPlayer)}: ${game.opening.fold(trans.analysis.txt())(_.opening.ecoName)}",
      // chat = views.html.chat.frag.some,
      moreCss = responsiveCssTag("analyse"),
      moreJs = frag(
        analyseTag,
        analyseNvuiTag,
        embedJs(s"""lidraughts=lidraughts||{};
lidraughts.analyse={data:${safeJsonValue(data)},i18n:${jsI18n()},userId:$jsUserId,chat:${jsOrNull(chatJson)},
explorer:{endpoint:"$explorerEndpoint",tablebaseEndpoint:"$tablebaseEndpoint"}}""")
      ),
      openGraph = povOpenGraph(pov).some
    )(frag(
        main(cls := "analyse")(
          st.aside(cls := "analyse__side")(
            views.html.game.side(pov, initialFen, none, simul = simul, userTv = userTv, bookmarked = bookmarked)
          ),
          div(cls := "analyse__board main-board")(draughtsgroundSvg),
          div(cls := "analyse__tools"),
          div(cls := "analyse__controls")
        ),
        if (ctx.blind) div(cls := "blind_content none")(
          h2("PDN downloads"),
          pdnLinks
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
              div(cls := "fen-pdn")(
                div(
                  strong("FEN"),
                  input(readonly := true, spellcheck := false, cls := "copyable autoselect analyse__underboard__fen")
                ),
                div(cls := "pdn-options")(
                  strong("PDN"),
                  pdnLinks
                ),
                div(cls := "pdn")(pdn)
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
              !game.isPdnImport option frag(
                game.turns > 1 option span(dataPanel := "move-times")(trans.moveTimes()),
                cross.isDefined option span(dataPanel := "ctable")(trans.crosstable())
              ),
              span(dataPanel := "fen-pdn")(raw("FEN &amp; PDN"))
            )
          ),
          div(cls := "analyse__underchat none")(views.html.round.bits underchat pov.game)
        )
      ))
  }
}
