package views.html.analyse

import controllers.routes
import play.api.libs.json.{ JsObject, Json }

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

object embed {

  import EmbedConfig.implicits._

  def apply(pov: lila.game.Pov, data: JsObject)(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = replay titleOf pov,
      cssModule = "analyse.embed"
    )(
      div(cls := "is2d")(
        main(cls := "analyse")
      ),
      footer {
        val url = routes.Round.watcher(pov.gameId, pov.color.name)
        frag(
          div(cls := "left")(
            a(target := "_blank", href := url)(h1(titleGame(pov.game))),
            " ",
            em("brought to you by ", a(target := "_blank", href := netBaseUrl)(netConfig.domain))
          ),
          a(target := "_blank", cls := "open", href := url)("Open")
        )
      },
      views.html.base.layout.lichessJsObject(config.nonce)(config.lang),
      depsTag,
      jsModule("analysis-board.embed"),
      analyseTag,
      embedJsUnsafeLoadThen(
        s"""analyseEmbed(${safeJsonValue(
          Json.obj(
            "data"  -> data,
            "embed" -> true,
            "i18n"  -> views.html.board.userAnalysisI18n(withCeval = false, withExplorer = false)
          )
        )})""",
        config.nonce
      )
    )

  def notFound(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = "404 - Game not found",
      cssModule = "analyse.embed"
    )(
      div(cls := "not-found")(
        h1("Game not found")
      )
    )
}
