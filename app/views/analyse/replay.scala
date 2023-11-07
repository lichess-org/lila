package views.html.analyse

import bits.dataPanel
import chess.format.Fen
import chess.format.pgn.PgnStr
import chess.variant.Crazyhouse
import controllers.routes
import play.api.i18n.Lang
import play.api.libs.json.Json

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue
import lila.game.Pov

object replay:

  private[analyse] def titleOf(pov: Pov)(using Lang) =
    s"${playerText(pov.game.whitePlayer)} vs ${playerText(pov.game.blackPlayer)}: ${pov.game.opening
        .fold(trans.analysis.txt())(_.opening.name)}"

  def apply(
      pov: Pov,
      data: play.api.libs.json.JsObject,
      initialFen: Option[chess.format.Fen.Epd],
      pgn: PgnStr,
      analysis: Option[lila.analyse.Analysis],
      analysisStarted: Boolean,
      simul: Option[lila.simul.Simul],
      cross: Option[lila.game.Crosstable.WithMatchup],
      userTv: Option[lila.user.User],
      chatOption: Option[lila.chat.UserChat.Mine],
      bookmarked: Boolean
  )(using ctx: PageContext) =

    import pov.*

    val chatJson = chatOption map { c =>
      views.html.chat.json(
        c.chat,
        name = trans.spectatorRoom.txt(),
        timeout = c.timeout,
        withNoteAge = ctx.isAuth option game.secondsSinceCreation,
        public = true,
        resourceId = lila.chat.Chat.ResourceId(s"game/${c.chat.id}"),
        palantir = ctx.canPalantir
      )
    }
    val imageLinks = frag(
      a(
        dataIcon := licon.NodeBranching,
        cls      := "text game-gif",
        targetBlank,
        href := cdnUrl(
          routes.Export.gif(pov.gameId, pov.color.name, ctx.pref.theme.some, ctx.pref.pieceSet.some).url
        )
      )(trans.gameAsGIF()),
      a(
        dataIcon := licon.NodeBranching,
        cls      := "text position-gif",
        targetBlank,
        href := cdnUrl(
          routes.Export
            .fenThumbnail(
              Fen.write(pov.game.situation).value,
              pov.color.name,
              None,
              pov.game.variant.key.some,
              ctx.pref.theme.some,
              ctx.pref.pieceSet.some
            )
            .url
        )
      )(trans.screenshotCurrentPosition())
    )
    val shareLinks = frag(
      a(dataIcon := licon.Expand, cls := "text embed-howto")(trans.embedInYourWebsite()),
      div(
        input(
          id         := "game-url",
          cls        := "copyable autoselect",
          spellcheck := "false",
          readonly,
          value := s"${netBaseUrl}${routes.Round.watcher(pov.gameId, pov.color.name)}"
        ),
        button(
          title    := "Copy URL",
          cls      := "copy button",
          dataRel  := "game-url",
          dataIcon := licon.Link
        )
      )
    )
    val pgnLinks = frag(
      a(
        dataIcon := licon.Download,
        cls      := "text",
        href     := s"${routes.Game.exportOne(game.id)}?literate=1",
        downloadAttr
      )(
        trans.downloadAnnotated()
      ),
      a(
        dataIcon := licon.Download,
        cls      := "text",
        href     := s"${routes.Game.exportOne(game.id)}?evals=0&clocks=0",
        downloadAttr
      )(
        trans.downloadRaw()
      ),
      game.isPgnImport option a(
        dataIcon := licon.Download,
        cls      := "text",
        href     := s"${routes.Game.exportOne(game.id)}?imported=1",
        downloadAttr
      )(trans.downloadImported())
    )

    bits.layout(
      title = titleOf(pov),
      moreCss = frag(
        cssTag("analyse.round"),
        pov.game.variant == Crazyhouse option cssTag("analyse.zh"),
        ctx.blind option cssTag("round.nvui")
      ),
      moreJs = frag(
        analyseNvuiTag,
        analyseInit(
          "replay",
          Json
            .obj(
              "data"   -> data,
              "i18n"   -> jsI18n(),
              "userId" -> ctx.userId,
              "chat"   -> chatJson
            )
            .add("hunter" -> isGranted(_.ViewBlurs)) ++
            views.html.board.bits.explorerAndCevalConfig
        )
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
              div(role := "tablist", cls := "analyse__underboard__menu")(
                game.analysable option
                  span(role := "tab", cls := "computer-analysis", dataPanel := "computer-analysis")(
                    trans.computerAnalysis()
                  ),
                !game.isPgnImport option frag(
                  game.ply > 1 option span(role := "tab", dataPanel := "move-times")(trans.moveTimes()),
                  cross.isDefined option span(role := "tab", dataPanel := "ctable")(trans.crosstable())
                ),
                span(role := "tab", dataPanel := "fen-pgn")(trans.study.shareAndExport())
              ),
              div(cls := "analyse__underboard__panels")(
                game.analysable option div(cls := "computer-analysis")(
                  if analysis.isDefined || analysisStarted then
                    div(id := "acpl-chart-container")(canvas(id := "acpl-chart"))
                  else
                    postForm(
                      cls    := s"future-game-analysis${ctx.isAnon so " must-login"}",
                      action := routes.Analyse.requestAnalysis(gameId)
                    )(
                      submitButton(cls := "button text")(
                        span(cls := "is3 text", dataIcon := licon.BarChart)(trans.requestAComputerAnalysis())
                      )
                    )
                ),
                div(cls := "move-times")(
                  game.ply > 1 option div(id := "movetimes-chart-container")(canvas(id := "movetimes-chart"))
                ),
                div(cls := "fen-pgn")(
                  div(
                    strong("FEN"),
                    input(
                      readonly,
                      spellcheck := false,
                      cls        := "copyable autoselect like-text analyse__underboard__fen"
                    )
                  ),
                  ctx.noBlind option div(
                    strong("Image"),
                    imageLinks
                  ),
                  div(
                    strong("Share"),
                    shareLinks
                  ),
                  div(
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
              )
            )
          )
        ),
        ctx.blind option div(cls := "blind-content none")(
          h2("PGN downloads"),
          pgnLinks,
          button(cls := "copy-pgn", attr("data-pgn") := pgn)(
            "Copy PGN to clipboard"
          )
        )
      )
    )
