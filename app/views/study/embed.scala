package views.html.study

import play.api.libs.json.Json

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._

object embed {

  import EmbedConfig.implicits.configLang

  def apply(
      s: lila.study.Study,
      chapter: lila.study.Chapter,
      data: lila.study.JsonView.JsData,
  )(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = s"${s.name} ${chapter.name}",
      moreCss = cssTag("embed.analyse"),
      moreJs = frag(
        translationJsTag("core"),
        moduleJsTag(
          "embed.analyse",
          Json.obj(
            "study" -> data.study,
            "data"  -> data.analysis,
          ),
          config.nonce.some,
        ),
      ),
      variant = chapter.setup.variant,
    )(
      div(main(cls := "analyse")),
    )

  def notFound(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = s"404 - ${trans.study.studyNotFound.txt()}",
      moreCss = cssTag("embed.analyse"),
    )(
      body(cls := "dark")(
        div(cls := "not-found")(
          h1(trans.study.studyNotFound()),
        ),
      ),
    )
}
