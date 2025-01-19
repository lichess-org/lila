package views.html.analyse

import play.api.libs.json.JsObject
import play.api.libs.json.Json

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._

object embed {

  import EmbedConfig.implicits.configLang

  def apply(pov: lila.game.Pov, data: JsObject)(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = s"${trans.noGameFound.txt()}",
      moreCss = cssTag("embed.analyse"),
      moreJs = frag(
        translationJsTag("core"),
        moduleJsTag(
          "embed.analyse",
          Json.obj(
            "data" -> data,
          ),
          config.nonce.some,
        ),
      ),
      variant = pov.game.variant,
    )(
      div(main(cls := "analyse")),
    )

  def notFound(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = trans.noGameFound.txt(),
      moreCss = cssTag("embed.analyse"),
    )(
      body(cls := "dark")(
        div(cls := "not-found")(
          h1(trans.noGameFound.txt()),
        ),
      ),
    )
}
