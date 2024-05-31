package views.html.analyse

import play.api.libs.json.{ JsObject, Json }

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import views.html.base.layout.{ bits => layout }

import controllers.routes

object embed {

  import EmbedConfig.implicits._

  def apply(pov: lila.game.Pov, data: JsObject)(implicit config: EmbedConfig) =
    frag(
      layout.doctype,
      layout.htmlTag(config.lang)(
        head(
          layout.charset,
          layout.viewport,
          layout.metaCsp(basicCsp withNonce config.nonce),
          st.headTitle(replay titleOf pov),
          if (pov.game.variant.chushogi) layout.chuPieceSprite(config.chuPieceSet)
          else if (pov.game.variant.kyotoshogi) layout.kyoPieceSprite(config.kyoPieceSet)
          else layout.pieceSprite(config.pieceSet),
          cssTagWithTheme("analyse.embed", config.bg)
        ),
        body(
          cls              := s"highlight ${config.bg} ${config.board}",
          dataDev          := (!isProd).option("true"),
          dataAssetUrl     := assetBaseUrl,
          dataAssetVersion := assetVersion.value,
          dataTheme        := config.bg,
          dataPieceSet     := config.pieceSet.key,
          dataChuPieceSet  := config.chuPieceSet.key,
          dataKyoPieceSet  := config.kyoPieceSet.key
        )(
          div(
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
          embedJsUnsafe(
            s"""lishogi.startEmbeddedAnalyse(${safeJsonValue(
                Json.obj(
                  "data"  -> data,
                  "embed" -> true,
                  "i18n"  -> views.html.board.userAnalysisI18n(withCeval = false)
                )
              )})""",
            config.nonce
          )
        )
      )
    )

  def notFound(implicit config: EmbedConfig) =
    frag(
      layout.doctype,
      layout.htmlTag(config.lang)(
        head(
          layout.charset,
          layout.viewport,
          layout.metaCsp(basicCsp),
          st.headTitle("404 - Game not found"),
          cssTagWithTheme("analyse.embed", "dark")
        ),
        body(cls := "dark")(
          div(cls := "not-found")(
            h1("Game not found")
          )
        )
      )
    )
}
