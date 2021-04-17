package views.html.analyse

import bits.dataPanel
import chess.variant.Crazyhouse
import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.game.Pov

object replay {

  private[analyse] def titleOf(pov: Pov)(implicit lang: Lang) =
    s"${playerText(pov.game.whitePlayer)} vs ${playerText(pov.game.blackPlayer)}: ${pov.game.opening
      .fold(trans.analysis.txt())(_.opening.ecoName)}"

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
      views.html.chat.json(
        c.chat,
        name = trans.spectatorRoom.txt(),
        timeout = c.timeout,
        withNoteAge = ctx.isAuth option game.secondsSinceCreation,
        public = true,
        resourceId = lila.chat.Chat.ResourceId(s"game/${c.chat.id}"),
        palantir = ctx.me.exists(_.canPalantir)
      )
    }
    val pgnLinks = div(
      a(
        dataIcon := "x",
        cls := "text",
        href := s"${routes.Game.exportOne(game.id)}?literate=1",
        downloadAttr
      )(
        trans.downloadAnnotated()
      ),
      a(
        dataIcon := "x",
        cls := "text",
        href := s"${routes.Game.exportOne(game.id)}?evals=0&clocks=0",
        downloadAttr
      )(
        trans.downloadRaw()
      ),
      game.isPgnImport option a(
        dataIcon := "x",
        cls := "text",
        href := s"${routes.Game.exportOne(game.id)}?imported=1",
        downloadAttr
      )(trans.downloadImported()),
      ctx.noBlind option frag(
        a(dataIcon := "=", cls := "text embed-howto")(trans.embedInYourWebsite()),
        a(
          dataIcon := "$",
          cls := "text",
          targetBlank,
          href := cdnUrl(routes.Export.gif(pov.gameId, pov.color.name).url)
        )(
          "Share as a GIF"
        )
      )
    )

    bits.layout(
      title = titleOf(pov),
      moreCss = frag(
        cssTag("analyse.round"),
        pov.game.variant == Crazyhouse option cssTag("analyse.zh"),
        ctx.blind option cssTag("round.nvui")
      ),
      moreJs = frag(
        analyseTag,
        analyseNvuiTag,
        embedJsUnsafeLoadThen(s"""LichessAnalyse.boot(${safeJsonValue(
          Json
            .obj(
              "data"   -> data,
              "i18n"   -> jsI18n(),
              "userId" -> ctx.userId,
              "chat"   -> chatJson,
              "explorer" -> Json.obj(
                "endpoint"          -> explorerEndpoint,
                "tablebaseEndpoint" -> tablebaseEndpoint
              )
            )
            .add("hunter" -> isGranted(_.Hunter))
        )})""")
      ),
      openGraph = povOpenGraph(pov).some
    )(
      frag(
        main(cls := "analyse")(
          st.aside(cls := "analyse__side")(
            views.html.game
              .side(
                pov,
                initialFen,
                none,
                simul = simul,
                userTv = userTv,
                bookmarked = bookmarked
              )
          ),
          chatOption.map(_ => views.html.chat.frag),
          div(cls := "analyse__board main-board")(chessgroundBoard),
          div(cls := "analyse__tools")(div(cls := "ceval")),
          div(cls := "analyse__controls"),
          !ctx.blind option frag(
            div(cls := "analyse__underboard")(
              div(cls := "analyse__underboard__panels")(
                game.analysable option div(cls := "computer-analysis")(
                  if (analysis.isDefined || analysisStarted) div(id := "acpl-chart")
                  else
                    postForm(
                      cls := s"future-game-analysis${ctx.isAnon ?? " must-login"}",
                      action := routes.Analyse.requestAnalysis(gameId)
                    )(
                      submitButton(cls := "button text")(
                        span(cls := "is3 text", dataIcon := "")(trans.requestAComputerAnalysis())
                      )
                    )
                ),
                div(cls := "move-times")(
                  game.turns > 1 option div(id := "movetimes-chart")
                ),
                div(cls := "fen-pgn")(
                  div(
                    strong("FEN"),
                    input(
                      readonly,
                      spellcheck := false,
                      cls := "copyable autoselect analyse__underboard__fen"
                    )
                  ),
                  div(cls := "pgn-options")(
                    strong("PGN"),
                    pgnLinks
                  ),
                  div(cls := "pgn")(pgn)
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
                    cls := "computer-analysis",
                    dataPanel := "computer-analysis"
                  )(trans.computerAnalysis()),
                !game.isPgnImport option frag(
                  game.turns > 1 option span(dataPanel := "move-times")(trans.moveTimes()),
                  cross.isDefined option span(dataPanel := "ctable")(trans.crosstable())
                ),
                span(dataPanel := "fen-pgn")(raw("FEN &amp; PGN"))
              )
            )
          )
        ),
        if (ctx.blind)
          div(cls := "blind-content none")(
            h2("PGN downloads"),
            pgnLinks
          )
      )
    )
  }
}
