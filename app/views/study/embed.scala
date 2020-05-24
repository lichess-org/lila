package views.html.study

import play.api.libs.json.Json
import play.api.mvc.RequestHeader

import lidraughts.app.templating.Environment._
import lidraughts.app.ui.EmbedConfig
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue
import lidraughts.i18n.{ I18nKeys => trans }
import views.html.base.layout.{ bits => layout }

import controllers.routes

object embed {

  import EmbedConfig.implicits._

  def apply(
    s: lidraughts.study.Study,
    chapter: lidraughts.study.Chapter,
    chapters: List[lidraughts.study.Chapter.IdName],
    data: lidraughts.study.JsonView.JsData
  )(implicit config: EmbedConfig) = frag(
    layout.doctype,
    layout.htmlTag(config.lang)(
      head(
        layout.charset,
        layout.viewport,
        layout.metaCsp(basicCsp withNonce config.nonce),
        st.headTitle(s"${s.name} ${chapter.name}"),
        layout.pieceSprite(lidraughts.pref.PieceSet.default),
        cssTagWithTheme("analyse.embed", config.bg)
      ),
      body(
        cls := s"highlight ${config.bg} ${config.board}",
        dataDev := (!isProd).option("true"),
        dataAssetUrl := assetBaseUrl,
        dataAssetVersion := assetVersion.value,
        dataTheme := config.bg
      )(
          div(cls := s"is2d is${chapter.setup.variant.boardSize.key}")(
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
              a(target := "_blank", cls := "open", dataIcon := "=", href := url, title := trans.study.open.txt())
            )
          },
          jQueryTag,
          jsTag("vendor/mousetrap.js"),
          jsAt("compiled/util.js"),
          jsAt("compiled/trans.js"),
          jsAt("compiled/embed-analyse.js"),
          analyseTag,
          embedJsUnsafe(s"""lidraughts.startEmbeddedAnalyse(${
            safeJsonValue(Json.obj(
              "study" -> data.study,
              "data" -> data.analysis,
              "embed" -> true,
              "i18n" -> views.html.board.userAnalysisI18n(),
              "userId" -> none[String]
            ))
          });
document.getElementById('chapter-selector').onchange = function() {
  location.href = this.value + location.search;
};""", config.nonce)
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
