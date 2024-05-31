package views.html.analyse

import play.api.i18n.Lang
import play.api.libs.json.Json

import bits.dataPanel
import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.game.{ Game, Pov }

import controllers.routes

object replay {

  private[analyse] def titleOf(pov: Pov)(implicit lang: Lang) =
    s"${playerText(pov.game.sentePlayer)} vs ${playerText(pov.game.gotePlayer)}: ${trans.analysis.txt()}"

  def apply(
      pov: Pov,
      data: play.api.libs.json.JsObject,
      kif: String,
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
    val exportLinks = div(
      ctx.noBlind option frag(
        Game.gifVariants.contains(pov.game.variant) option a(
          dataIcon := "$",
          cls      := "text",
          target   := "_blank",
          href     := cdnUrl(routes.Export.gif(pov.gameId, pov.color.name).url)
        )(
          "GIF"
        ),
        a(dataIcon := "=", cls := "text embed-howto", target := "_blank")(
          trans.embedInYourWebsite()
        )
      )
    )
    val kifLinks = div(
      span(
        a(
          dataIcon := "x",
          cls      := "text",
          href     := s"${routes.Game.exportOne(game.id)}?clocks=0&evals=0"
        )(
          trans.downloadRaw()
        ),
        a(
          dataIcon := "x",
          cls      := "text jis",
          href     := s"${routes.Game.exportOne(game.id)}?clocks=0&evals=0&shiftJis=1"
        )(
          "Shift-JIS"
        )
      ),
      a(dataIcon := "x", cls := "text", href := s"${routes.Game.exportOne(game.id)}?literate=1")(
        trans.downloadAnnotated()
      ),
      game.isKifImport option a(
        dataIcon := "x",
        cls      := "text",
        href     := s"${routes.Game.exportOne(game.id)}?imported=1"
      )(
        trans.downloadImported()
      )
    )
    val csaLinks = pov.game.variant.standard option div(
      a(
        dataIcon := "x",
        cls      := "text",
        href     := s"${routes.Game.exportOne(game.id)}?csa=1&clocks=0"
      )(
        trans.downloadRaw()
      ),
      a(dataIcon := "x", cls := "text", href := s"${routes.Game.exportOne(game.id)}?literate=1&csa=1")(
        trans.downloadAnnotated()
      ),
      game.isCsaImport option a(
        dataIcon := "x",
        cls      := "text",
        href     := s"${routes.Game.exportOne(game.id)}?imported=1&csa=1"
      )(trans.downloadImported())
    )

    bits.layout(
      title = titleOf(pov),
      moreCss = frag(
        cssTag("analyse.round"),
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
              "hunter" -> isGranted(_.Hunter)
            )
          )}""")
      ),
      openGraph = povOpenGraph(pov).some
    )(
      frag(
        main(cls := s"analyse ${mainVariantClass(pov.game.variant)}")(
          st.aside(cls := "analyse__side")(
            views.html.game
              .side(
                pov,
                none,
                simul = simul,
                userTv = userTv,
                bookmarked = bookmarked
              )
          ),
          chatOption.map(_ => views.html.chat.frag),
          div(cls := s"analyse__board main-board ${variantClass(pov.game.variant)}")(
            shogigroundEmpty(pov.game.variant, pov.color)
          ),
          div(cls := "analyse__tools")(div(cls := "ceval")),
          div(cls := "analyse__controls"),
          !ctx.blind option frag(
            div(cls := "analyse__underboard")(
              div(cls := "analyse__underboard__panels")(
                game.analysable option div(cls := "computer-analysis")(
                  if (analysis.isDefined || analysisStarted) div(id := "acpl-chart")
                  else
                    postForm(
                      cls    := s"future-game-analysis${ctx.isAnon ?? " must-login"}",
                      action := routes.Analyse.requestAnalysis(gameId)
                    )(
                      submitButton(cls := "button text")(
                        span(cls := "is3 text", dataIcon := "")(trans.requestAComputerAnalysis())
                      )
                    )
                ),
                div(cls := "move-times")(
                  (game.plies > 1 && !game.isNotationImport) option div(id := "movetimes-chart")
                ),
                div(cls := "sfen-notation")(
                  div(
                    strong("SFEN"),
                    input(
                      readonly,
                      spellcheck := false,
                      cls        := "copyable autoselect analyse__underboard__sfen"
                    )
                  ),
                  div(cls := "notation-options")(
                    strong("KIF"),
                    kifLinks
                  ),
                  csaLinks map { csa =>
                    div(cls := "notation-options")(
                      strong("CSA"),
                      csa
                    )
                  },
                  div(cls := "notation-options")(
                    strong(trans.export()),
                    exportLinks
                  ),
                  div(cls := "kif")(kif)
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
                    cls       := "computer-analysis",
                    dataPanel := "computer-analysis",
                    title := analysis.map { a =>
                      s"Provided by ${usernameOrId(a.providedBy)}"
                    }
                  )(trans.computerAnalysis()),
                !game.isNotationImport option frag(
                  (game.plies > 1 && !game.isCorrespondence) option span(dataPanel := "move-times")(
                    trans.moveTimes()
                  ),
                  cross.isDefined option span(dataPanel := "ctable")(trans.crosstable())
                ),
                span(dataPanel := "sfen-notation")(trans.export())
              )
            )
          ),
          div(cls := "analyse__acpl")
        ),
        if (ctx.blind)
          div(cls := "blind-content none")(
            h2("KIF/CSA"),
            kifLinks,
            csaLinks
          )
      )
    )
  }
}
