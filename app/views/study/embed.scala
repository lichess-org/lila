package views.html.study

import play.api.libs.json.Json
import play.api.mvc.RequestHeader

import lidraughts.app.templating.Environment._
import lidraughts.app.ui.EmbedConfig
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue
import views.html.base.layout.{ bits => layout }

import controllers.routes

object embed {

  import EmbedConfig.implicits._

  def apply(s: lidraughts.study.Study, chapter: lidraughts.study.Chapter, data: lidraughts.study.JsonView.JsData)(implicit config: EmbedConfig) = frag(
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
          div(cls := "is2d")(
            main(cls := "analyse")
          ),
          footer {
            val url = routes.Study.chapter(s.id.value, chapter.id.value)
            frag(
              div(cls := "left")(
                a(target := "_blank", href := url)(h1(s.name.value)),
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
          embedJsUnsafe(s"""lidraughts.startEmbeddedAnalyse(${
            safeJsonValue(Json.obj(
              "study" -> data.study,
              "data" -> data.analysis,
              "embed" -> true,
              "i18n" -> views.html.board.userAnalysisI18n(),
              "userId" -> none[String]
            ))
          })""", config.nonce)
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
        st.headTitle("404 - Study not found"),
        cssTagWithTheme("analyse.round.embed", "dark")
      ),
      body(cls := "dark")(
        div(cls := "not-found")(
          h1("Study not found")
        )
      )
    )
  )
}
