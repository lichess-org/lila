package views.html.analyse

import chess.variant.Standard
import play.api.i18n.Lang
import play.api.libs.json.Json
import play.utils.UriEncoding

import bits.dataPanel
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.game.Pov

import controllers.routes

object replay {

  private[analyse] def titleOf(pov: Pov)(implicit lang: Lang) =
    s"${playerText(pov.game.whitePlayer)} vs ${playerText(pov.game.blackPlayer)}: ${pov.game.opening
      .fold(trans.analysis.txt())(_.opening.ecoName)}"

  def apply(
      pov: Pov,
      data: play.api.libs.json.JsObject,
      initialFen: Option[chess.format.FEN],
      kifu: String,
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
      /*
      a(dataIcon := "x", cls := "text", href := s"${routes.Game.exportOne(game.id)}?literate=1")(
        trans.downloadAnnotated()
      ),*/
      game.variant == chess.variant.Standard option a(dataIcon := "x", cls := "text", href := s"data:text/plain;charset=utf-8,${UriEncoding.encodePathSegment(kifu, "UTF-8")}", attr("download") := s"${game.createdAt}-${game.whitePlayer.userId | "Anonymous"}-vs-${game.blackPlayer.userId | "Anonymous"}.kifu")(
        trans.downloadRaw()
      ),
      game.isPgnImport option a(
        dataIcon := "x",
        cls := "text",
        href := s"${routes.Game.exportOne(game.id)}?imported=1"
      )(trans.downloadImported()),
      ctx.noBlind option frag(
        a(dataIcon := "=", cls := "text embed-howto", target := "_blank")(
          trans.embedInYourWebsite()
        )
        /*a(
          dataIcon := "$",
          cls := "text",
          target := "_blank",
          href := cdnUrl(routes.Page.notSupported().url) // routes.Export.gif(pov.gameId, pov.color.name).url
        )(
          "Share as a GIF"
        )*/
      )
    )

    bits.layout(
      title = titleOf(pov),
      moreCss = frag(
        cssTag("analyse.round"),
        cssTag("analyse.zh"), //pov.game.variant == Standard option cssTag("analyse.zh"),
        ctx.blind option cssTag("round.nvui")
      ),
      moreJs = frag(
        analyseTag,
        analyseNvuiTag,
        embedJsUnsafe(s"""lishogi=lishogi||{};lishogi.analyse=${safeJsonValue(
          Json.obj(
            "data"   -> data,
            "i18n"   -> jsI18n(),
            "userId" -> ctx.userId,
            "chat"   -> chatJson,
            "explorer" -> Json.obj(
              "endpoint"          -> explorerEndpoint,
              "tablebaseEndpoint" -> tablebaseEndpoint
            ),
            "hunter" -> isGranted(_.Hunter)
          )
        )}""")
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
          div(cls := "analyse__board main-board")(shogigroundBoard),
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
                    strong("SFEN"),
                    input(
                      readonly,
                      spellcheck := false,
                      cls := "copyable autoselect analyse__underboard__fen"
                    )
                  ),
                  div(cls := "pgn-options")(
                    strong("Kifu"),
                    pgnLinks
                  ),
                  game.variant == chess.variant.Standard option div(cls := "pgn")(kifu)
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
                    dataPanel := "computer-analysis",
                    title := analysis.map { a =>
                      s"Provided by ${usernameOrId(a.providedBy)}"
                    }
                  )(trans.computerAnalysis()),
                !game.isPgnImport option frag(
                  game.turns > 1 option span(dataPanel := "move-times")(trans.moveTimes()),
                  cross.isDefined option span(dataPanel := "ctable")(trans.crosstable())
                ),
                span(dataPanel := "fen-pgn")(raw("SFEN &amp; Kifu"))
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
