package views.html.study

import controllers.routes
import play.api.libs.json.Json

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.EmbedConfig
import lila.app.ui.EmbedConfig.given
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.String.html.safeJsonValue
import lila.i18n.{ I18nKeys as trans }

object embed:

  def apply(
      s: lila.study.Study,
      chapter: lila.study.Chapter,
      chapters: List[lila.study.Chapter.IdName],
      data: lila.study.JsonView.JsData
  )(using config: EmbedConfig) =
    views.html.base.embed(
      title = s"${s.name} ${chapter.name}",
      cssModule = "analyse.embed"
    )(
      div(cls := "is2d")(
        main(cls := "analyse")
      ),
      footer {
        val url = routes.Study.chapter(s.id, chapter.id)
        frag(
          div(cls := "left")(
            select(id := "chapter-selector")(chapters.map { c =>
              option(
                value := c.id,
                (c.id == chapter.id) option selected
              )(c.name)
            }),
            a(targetBlank, href := url)(h1(s.name))
          ),
          a(
            targetBlank,
            cls      := "open",
            dataIcon := licon.Expand,
            href     := url,
            title    := trans.study.open.txt()
          )
        )
      },
      views.html.base.layout.inlineJs(config.nonce)(using config.lang),
      depsTag,
      jsModule("analysisBoard.embed"),
      analyseStudyTag,
      embedJsUnsafeLoadThen(
        s"""analyseEmbed(${safeJsonValue(
            Json.obj(
              "study"  -> data.study,
              "data"   -> data.analysis,
              "embed"  -> true,
              "i18n"   -> jsI18n.embed(chapter),
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
