package views.html.analyse

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object embed {

  import views.html.base.layout.bits._

  private def bodyClass(implicit ctx: Context) = List(
    "base" -> true,
    ctx.currentTheme.cssClass -> true,
    (if (ctx.currentBg == "transp") "dark transp" else ctx.currentBg) -> true
  )

  def apply(pov: lila.game.Pov, data: play.api.libs.json.JsObject)(implicit ctx: Context) = frag(
    doctype,
    htmlTag(ctx)(
      topComment,
      head(
        charset,
        metaCsp(none),
        st.headTitle(s"${playerText(pov.game.whitePlayer)} vs ${playerText(pov.game.blackPlayer)} in ${pov.gameId} : ${pov.game.opening.fold(trans.analysis.txt())(_.opening.ecoName)}"),
        // fontStylesheets, // use proper stylesheets instead
        // currentBgCss,
        cssTags("common.css", "board.css", "analyse.css", "analyse-embed.css"),
        pieceSprite
      ),
      body(cls := bodyClass ::: List(
        "highlight" -> true,
        "piece-letter" -> ctx.pref.pieceNotationIsLetter
      ))(
        div(cls := "is2d")(
          div(cls := "embedded_analyse analyse cg-512")(miniBoardContent)
        ),
        footer {
          val url = routes.Round.watcher(pov.gameId, pov.color.name)
          frag(
            div(cls := "left")(
              a(target := "_blank", href := url)(h1(titleGame(pov.game))),
              " ",
              em("brought to you by ", a(target := "_blank", href := netBaseUrl)(netDomain))
            ),
            a(target := "_blank", cls := "open", href := url)("Open")
          )
        },
        jQueryTag,
        jsTag("vendor/mousetrap.js"),
        jsAt("compiled/util.js"),
        jsAt("compiled/trans.js"),
        analyseTag,
        jsTag("embed-analyse.js"),
        embedJs(s"""lichess.startEmbeddedAnalyse({
element: document.querySelector('.embedded_analyse'),
data: ${safeJsonValue(data)},
embed: true,
i18n: ${views.html.board.userAnalysisI18n(withCeval = false, withExplorer = false)}
});""")
      )
    )
  )

  def notFound()(implicit ctx: Context) = frag(
    doctype,
    htmlTag(ctx)(
      topComment,
      head(
        charset,
        metaCsp(none),
        st.headTitle("404 - Game not found"),
        // fontStylesheets, // use proper stylesheets instead
        // currentBgCss,
        cssTags("common.css", "analyse-embed.css")
      ),
      body(cls := bodyClass)(
        div(cls := "not_found")(
          h1("Game not found")
        )
      )
    )
  )
}
