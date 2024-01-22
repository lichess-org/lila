package views.html.study

import play.api.libs.json.Json

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.i18n.{ I18nKeys => trans }
import views.html.base.layout.{ bits => layout }

import controllers.routes

object embed {

  import EmbedConfig.implicits._

  def apply(
      s: lila.study.Study,
      chapter: lila.study.Chapter,
      chapters: List[lila.study.Chapter.IdName],
      data: lila.study.JsonView.JsData
  )(implicit config: EmbedConfig) =
    frag(
      layout.doctype,
      layout.htmlTag(config.lang)(
        head(
          layout.charset,
          layout.viewport,
          layout.metaCsp(basicCsp withNonce config.nonce),
          st.headTitle(s"${s.name} ${chapter.name}"),
          layout.pieceSprite(config.pieceSet),
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
            val url = routes.Study.chapter(s.id.value, chapter.id.value)
            frag(
              div(cls := "left")(
                select(id := "chapter-selector")(chapters.map { c =>
                  option(
                    value := c.id.value,
                    (c.id == chapter.id) option selected
                  )(c.name.value)
                }),
                a(target := "_blank", href := url)(h1(s.name.value))
              ),
              a(
                target   := "_blank",
                cls      := "open",
                dataIcon := "=",
                href     := url,
                title    := trans.study.open.txt()
              )
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
                  "study"  -> data.study,
                  "data"   -> data.analysis,
                  "embed"  -> true,
                  "i18n"   -> views.html.board.userAnalysisI18n(),
                  "userId" -> none[String]
                )
              )});
document.getElementById('chapter-selector').onchange = function() {
  location.href = this.value + location.search;
};""",
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
          st.headTitle(s"404 - ${trans.study.studyNotFound.txt()}"),
          cssTagWithTheme("analyse.embed", "dark")
        ),
        body(cls := "dark")(
          div(cls := "not-found")(
            h1(trans.study.studyNotFound())
          )
        )
      )
    )
}
