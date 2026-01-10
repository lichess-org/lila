package lila.analyse
package ui

import chess.variant.*
import chess.format.Fen
import chess.format.pgn.PgnStr
import play.api.libs.json.*

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.common.Json.given

final class ReplayUi(helpers: Helpers)(analyseUi: AnalyseUi):
  import helpers.{ *, given }
  import analyseUi.bits.dataPanel

  def forCrawler(
      pov: Pov,
      pgn: PgnStr,
      graph: OpenGraph,
      gameSide: Option[Frag],
      crosstable: Option[Tag]
  )(using Context) =
    Page(analyseUi.titleOf(pov))
      .css("analyse.round")
      .graph(graph)
      .csp(analyseUi.bits.cspExternalEngine)
      .flag(_.noRobots):
        main(cls := "analyse")(
          st.aside(cls := "analyse__side")(gameSide),
          div(cls := "analyse__board main-board")(chessgroundBoard),
          div(cls := "analyse__tools")(div(cls := "ceval")),
          div(cls := "analyse__controls"),
          div(cls := "analyse__underboard")(
            div(cls := "analyse__underboard__panels")(
              div(cls := "fen-pgn active")(
                div(
                  strong("FEN"),
                  input(readonly, spellcheck := false, cls := "analyse__underboard__fen")
                ),
                div(cls := "pgn")(pgn)
              ),
              crosstable.map(div(cls := "ctable active")(_))
            )
          )
        )

  def forBrowser(
      pov: Pov,
      data: JsObject,
      pgn: PgnStr,
      analysable: Boolean,
      hasAnalysis: Boolean,
      graph: OpenGraph,
      gameSide: Option[Frag],
      crosstable: Option[Tag],
      chatOption: Option[(JsObject, Frag)]
  )(using ctx: Context) =

    import pov.*

    val imageLinks = frag(
      copyMeLink(
        cdnUrl(
          routes.Export.gif(pov.gameId, pov.color, ctx.pref.theme.some, ctx.pref.pieceSet.some).url
        ),
        trans.site.gameAsGIF()
      )(cls := "game-gif"),
      copyMeLink(
        fenThumbnailUrl(Fen.write(pov.game.position).opening, pov.color.some, pov.game.variant),
        trans.site.screenshotCurrentPosition()
      )(cls := "position-gif")
    )

    val shareLinks = frag(
      a(dataIcon := Icon.Expand, cls := "text embed-howto")(trans.site.embedInYourWebsite()),
      copyMeInput(routeUrl(routes.Round.watcher(pov.gameId, pov.color)).value)
    )
    val pgnLinks = frag(
      copyMeContent(pathUrl(s"${routes.Game.exportOne(game.id)}?literate=1"), trans.site.downloadAnnotated()),
      copyMeContent(pathUrl(s"${routes.Game.exportOne(game.id)}?evals=0&clocks=0"), trans.site.downloadRaw()),
      game.isPgnImport.option:
        copyMeContent(pathUrl(s"${routes.Game.exportOne(game.id)}?imported=1"), trans.site.downloadImported())
    )

    analyseUi.bits
      .page(analyseUi.titleOf(pov))
      .css("analyse.round")
      .css((pov.game.variant == Crazyhouse).option("analyse.zh"))
      .css(ctx.blind.option("round.nvui"))
      .css(ctx.pref.hasKeyboardMove.option("keyboardMove"))
      .i18n(_.study)
      .i18nOpt(ctx.speechSynthesis, _.nvui)
      .i18nOpt(ctx.blind, _.keyboardMove, _.nvui)
      .js(analyseNvuiTag)
      .js:
        analyseUi.bits.analyseModule(
          "replay",
          Json
            .obj(
              "data" -> data,
              "userId" -> ctx.userId,
              "chat" -> chatOption._1F
            )
            .add("hunter" -> Granter.opt(_.ViewBlurs)) ++
            analyseUi.explorerAndCevalConfig
        )
      .graph(graph):
        frag(
          main(cls := "analyse")(
            st.aside(cls := "analyse__side")(gameSide),
            chatOption._2F,
            div(cls := "analyse__board main-board")(chessgroundBoard),
            div(cls := "analyse__tools")(div(cls := "ceval")),
            div(cls := "analyse__controls"),
            ctx.blind.not.option(
              frag(
                div(cls := "analyse__underboard")(
                  div(role := "tablist", cls := "analyse__underboard__menu")(
                    analysable.option(
                      span(
                        role := "tab",
                        cls := "computer-analysis",
                        dataPanel := "computer-analysis",
                        textAndTitle(trans.site.computerAnalysis)
                      )
                    ),
                    (!game.isPgnImport).option(
                      frag(
                        (game.ply > 1).option(
                          span(role := "tab", dataPanel := "move-times", textAndTitle(trans.site.moveTimes))
                        ),
                        crosstable.isDefined.option:
                          span(role := "tab", dataPanel := "ctable", textAndTitle(trans.site.crosstable))
                      )
                    ),
                    span(role := "tab", dataPanel := "fen-pgn", textAndTitle(trans.study.shareAndExport))
                  ),
                  div(cls := "analyse__underboard__panels")(
                    analysable.option(
                      div(cls := "computer-analysis")(
                        if hasAnalysis then div(id := "acpl-chart-container")(canvas(id := "acpl-chart"))
                        else
                          postForm(
                            cls := s"future-game-analysis${ctx.isAuth.not.so(" must-login")}",
                            action := routes.Analyse.requestAnalysis(gameId)
                          ):
                            submitButton(cls := "button text"):
                              span(cls := "is3 text", dataIcon := Icon.BarChart)(
                                trans.site.requestAComputerAnalysis()
                              )
                      )
                    ),
                    div(cls := "move-times")(
                      (game.ply > 1)
                        .option(div(id := "movetimes-chart-container")(canvas(id := "movetimes-chart")))
                    ),
                    div(cls := "fen-pgn")(
                      div(
                        strong("FEN"),
                        copyMeInput("")(cls := "analyse__underboard__fen")
                      ),
                      div(
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
                    crosstable.map(div(cls := "ctable")(_))
                  )
                )
              )
            )
          ),
          ctx.blind.option:
            div(cls := "blind-content none")(
              h2(trans.nvui.pgnAndFen()),
              button(cls := "copy-pgn", attr("data-pgn") := pgn):
                trans.nvui.copyToClipboard("PGN")
              ,
              button(cls := "copy-fen"):
                trans.nvui.copyToClipboard("FEN")
              ,
              pgnLinks,
              div(
                "FEN",
                copyMeInput("")(cls := "analyse__underboard__fen")
              )
            )
        )
