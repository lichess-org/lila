package views.analyse

import chess.format.Fen
import chess.format.pgn.PgnStr
import chess.variant.Crazyhouse
import play.api.libs.json.Json
import views.analyse.bits.dataPanel

import lila.app.UiEnv.{ *, given }
import lila.common.Json.given
import lila.round.RoundGame.secondsSinceCreation

def replay(
    pov: Pov,
    data: play.api.libs.json.JsObject,
    initialFen: Option[chess.format.Fen.Full],
    pgn: PgnStr,
    analysis: Option[lila.analyse.Analysis],
    analysisStarted: Boolean,
    simul: Option[lila.simul.Simul],
    cross: Option[lila.game.Crosstable.WithMatchup],
    userTv: Option[User],
    chatOption: Option[lila.chat.UserChat.Mine],
    bookmarked: Boolean
)(using ctx: Context) =

  import pov.*

  val chatJson = chatOption.map: c =>
    views.chat.json(
      c.chat,
      c.lines,
      name = trans.site.spectatorRoom.txt(),
      timeout = c.timeout,
      withNoteAge = ctx.isAuth.option(game.secondsSinceCreation),
      public = true,
      resourceId = lila.chat.Chat.ResourceId(s"game/${c.chat.id}"),
      palantir = ctx.canPalantir
    )
  val imageLinks = frag(
    copyMeLink(
      cdnUrl(
        routes.Export.gif(pov.gameId, pov.color, ctx.pref.theme.some, ctx.pref.pieceSet.some).url
      ),
      trans.site.gameAsGIF()
    )(cls := "game-gif"),
    copyMeLink(
      cdnUrl(
        routes.Export
          .fenThumbnail(
            Fen.write(pov.game.situation).value,
            pov.color.some,
            None,
            pov.game.variant.key.some,
            ctx.pref.theme.some,
            ctx.pref.pieceSet.some
          )
          .url
      ),
      trans.site.screenshotCurrentPosition()
    )(cls := "position-gif")
  )

  val shareLinks = frag(
    a(dataIcon := Icon.Expand, cls := "text embed-howto")(trans.site.embedInYourWebsite()),
    copyMeInput(s"${netBaseUrl}${routes.Round.watcher(pov.gameId, pov.color)}")
  )
  val pgnLinks = frag(
    copyMeLink(s"${routes.Game.exportOne(game.id)}?literate=1", trans.site.downloadAnnotated()),
    copyMeLink(s"${routes.Game.exportOne(game.id)}?evals=0&clocks=0", trans.site.downloadRaw()),
    game.isPgnImport.option:
      copyMeLink(s"${routes.Game.exportOne(game.id)}?imported=1", trans.site.downloadImported())
  )

  bits
    .page(ui.titleOf(pov))
    .css("analyse.round")
    .css((pov.game.variant == Crazyhouse).option("analyse.zh"))
    .css(ctx.blind.option("round.nvui"))
    .css(ctx.pref.hasKeyboardMove.option("keyboardMove"))
    .js(analyseNvuiTag)
    .js(
      bits.analyseModule(
        "replay",
        Json
          .obj(
            "data"   -> data,
            "i18n"   -> views.analysisI18n(),
            "userId" -> ctx.userId,
            "chat"   -> chatJson
          )
          .add("hunter" -> isGranted(_.ViewBlurs)) ++
          views.board.explorerAndCevalConfig
      )
    )
    .graph(views.round.ui.povOpenGraph(pov)):
      frag(
        main(cls := "analyse")(
          st.aside(cls := "analyse__side")(
            views.game
              .side(
                pov,
                initialFen,
                none,
                simul = simul,
                userTv = userTv,
                bookmarked = bookmarked
              )
          ),
          chatOption.map(_ => views.chat.frag),
          div(cls := "analyse__board main-board")(chessgroundBoard),
          div(cls := "analyse__tools")(div(cls := "ceval")),
          div(cls := "analyse__controls"),
          (!ctx.blind).option(
            frag(
              div(cls := "analyse__underboard")(
                div(role := "tablist", cls := "analyse__underboard__menu")(
                  lila.game.GameExt
                    .analysable(game)
                    .option(
                      span(role := "tab", cls := "computer-analysis", dataPanel := "computer-analysis")(
                        trans.site.computerAnalysis()
                      )
                    ),
                  (!game.isPgnImport).option(
                    frag(
                      (game.ply > 1)
                        .option(span(role := "tab", dataPanel := "move-times")(trans.site.moveTimes())),
                      cross.isDefined.option(
                        span(role := "tab", dataPanel := "ctable")(trans.site.crosstable())
                      )
                    )
                  ),
                  span(role := "tab", dataPanel := "fen-pgn")(trans.study.shareAndExport())
                ),
                div(cls := "analyse__underboard__panels")(
                  lila.game.GameExt
                    .analysable(game)
                    .option(
                      div(cls := "computer-analysis")(
                        if analysis.isDefined || analysisStarted then
                          div(id := "acpl-chart-container")(canvas(id := "acpl-chart"))
                        else
                          postForm(
                            cls    := s"future-game-analysis${ctx.isAnon.so(" must-login")}",
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
                    ctx.noBlind.option(
                      div(
                        strong("Image"),
                        imageLinks
                      )
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
                  cross.map: c =>
                    div(cls := "ctable"):
                      views.game.ui.crosstable(pov.player.userId.fold(c)(c.fromPov), pov.gameId.some)
                )
              )
            )
          )
        ),
        ctx.blind.option(
          div(cls := "blind-content none")(
            h2("PGN downloads"),
            pgnLinks,
            button(cls := "copy-pgn", attr("data-pgn") := pgn):
              "Copy PGN to clipboard"
          )
        )
      )
