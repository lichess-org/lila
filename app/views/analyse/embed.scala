package views.html.analyse

import play.api.libs.json.JsObject
import play.api.mvc.RequestHeader

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import views.html.base.layout.{ bits => layout }

import controllers.routes

object embed {

  import EmbedConfig.implicits._

  def apply(pov: lila.game.Pov, data: JsObject)(implicit config: EmbedConfig) = frag(
    layout.doctype,
    layout.htmlTag(config.lang)(
      head(
        layout.charset,
        layout.viewport,
        layout.metaCsp(basicCsp withNonce config.nonce),
        st.headTitle(replay titleOf pov),
        layout.pieceSprite(lila.pref.PieceSet.default),
        cssTagWithTheme("analyse.embed", config.bg)
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
        jsAt("compiled/embed-analyse.js"),
        analyseTag,
        embedJsUnsafe(s"""lichess.startEmbeddedAnalyse({
data: ${safeJsonValue(data)},
embed: true,
i18n: ${views.html.board.userAnalysisI18n(withCeval = false, withExplorer = false)}
});""", config.nonce)
      )
    )
  )

  def notFound(implicit config: EmbedConfig) = frag(
    layout.doctype,
    layout.htmlTag(config.lang)(
      head(
        layout.charset,
        layout.viewport,
        layout.metaCsp(basicCsp),
        st.headTitle("404 - Game not found"),
        cssTagWithTheme("analyse.round.embed", "dark")
      ),
      body(cls := "dark")(
        div(cls := "not-found")(
          h1("Game not found")
        )
      )
    )
  )
}
