package views.html.analyse

import play.api.libs.json.JsObject
import play.api.mvc.RequestHeader

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._
import lila.common.Lang
import lila.common.String.html.safeJsonValue

import controllers.routes

object embed {

  import views.html.base.layout.bits._
  import EmbedConfig.implicits._

  def apply(pov: lila.game.Pov, data: JsObject)(implicit config: EmbedConfig) = frag(
    doctype,
    htmlTag(config.lang)(
      topComment,
      head(
        charset,
        viewport,
        metaCsp(basicCsp withNonce config.nonce),
        st.headTitle(replay titleOf pov),
        pieceSprite(lila.pref.PieceSet.default),
        responsiveCssTagWithTheme("analyse.round.embed", config.bg)
      ),
      body(cls := List(
        s"highlight ${config.bg} ${config.board}" -> true
      ))(
        div(cls := "is2d")(
          main(cls := "analyse")
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
data: ${safeJsonValue(data)},
embed: true,
i18n: ${views.html.board.userAnalysisI18n(withCeval = false, withExplorer = false)}
});""", config.nonce)
      )
    )
  )

  def notFound(implicit config: EmbedConfig) = frag(
    doctype,
    htmlTag(config.lang)(
      topComment,
      head(
        charset,
        viewport,
        metaCsp(basicCsp),
        st.headTitle("404 - Game not found"),
        responsiveCssTagWithTheme("analyse.round.embed", "dark")
      ),
      body(cls := "dark")(
        div(cls := "not-found")(
          h1("Game not found")
        )
      )
    )
  )
}
