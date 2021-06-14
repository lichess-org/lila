package views.html.study

import controllers.routes
import play.api.libs.json.Json

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.i18n.{ I18nKeys => trans }

object embed {

  import EmbedConfig.implicits._

  def apply(
      s: lila.study.Study,
      chapter: lila.study.Chapter,
      chapters: List[lila.study.Chapter.IdName],
      data: lila.study.JsonView.JsData
  )(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = s"${s.name} ${chapter.name}",
      cssModule = "analyse.embed"
    )(
      div(cls := "is2d")(
        main(cls := "analyse")
      ),
      footer {
        val url = routes.Study.chapter(s.id.value, chapter.id.value)
        frag(
          div(cls := "left")(
            select(id := "chapter-selector")(chapters.map { c =>
              option(
                value := c.id.value,
                (c.id == chapter.id) option selected
              )(c.name.value)
            }),
            a(targetBlank, href := url)(h1(s.name.value))
          ),
          a(
            targetBlank,
            cls := "open",
            dataIcon := "",
            href := url,
            title := trans.study.open.txt()
          )
        )
      },
      views.html.base.layout.lichessJsObject(config.nonce)(config.lang),
      depsTag,
      jsModule("analysisBoard.embed"),
      analyseTag,
      embedJsUnsafeLoadThen(
        s"""analyseEmbed(${safeJsonValue(
          Json.obj(
            "study"  -> data.study,
            "data"   -> data.analysis,
            "embed"  -> true,
            "i18n"   -> views.html.board.userAnalysisI18n(),
            "userId" -> none[String]
          )
        )});
document.getElementById('chapter-selector').onchange = function() {
  location.href = this.value + location.search;
}""",
        config.nonce
      )
    )

  def notFound(implicit config: EmbedConfig) =
    views.html.base.embed(
      title = s"404 - ${trans.study.studyNotFound.txt()}",
      cssModule = "analyse.embed"
    )(
      div(cls := "not-found")(
        h1(trans.study.studyNotFound())
      )
    )
}
