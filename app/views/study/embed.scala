package views.html.study

import play.api.libs.json.Json
import play.api.mvc.RequestHeader

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.common.Lang

import controllers.routes

object embed {

  import views.html.base.layout.bits._

  // private def bodyClass(implicit ctx: Context) = List(
  //   "base" -> true,
  //   ctx.currentTheme.cssClass -> true,
  //   (if (ctx.currentBg == "transp") "dark transp" else ctx.currentBg) -> true
  // )

  private implicit def lang(implicit req: RequestHeader): Lang = lila.i18n.I18nLangPicker(req, none)

  def apply(s: lila.study.Study, chapter: lila.study.Chapter, data: lila.study.JsonView.JsData)(implicit req: RequestHeader) = frag(
    doctype,
    htmlTag(lang)( // topComment,
    // head(
    //   charset,
    //   metaCsp(basicCsp),
    //   st.headTitle(s"${s.name} ${chapter.name}"),
    //   // fontStylesheets, // use proper stylesheets instead
    //   // currentBgCss,
    //   cssTags("common.css", "board.css", "analyse.css", "analyse-embed.css"),
    //   pieceSprite
    // ),
    // body(cls := bodyClass ::: List(
    //   "highlight" -> true,
    //   "piece-letter" -> ctx.pref.pieceNotationIsLetter
    // ))(
    //   div(cls := "is2d")(
    //     div(cls := "embedded_study analyse cg-512")(miniBoardContent)
    //   ),
    //   footer {
    //     val url = routes.Study.chapter(s.id.value, chapter.id.value)
    //     div(cls := "left")(
    //       a(target := "_blank", href := url)(h1(s.name.value)),
    //       " ",
    //       em("brought to you by ", a(target := "_blank", href := netBaseUrl)(netDomain))
    //     )
    //     a(target := "_blank", cls := "open", href := url)("Open")
    //   },
    //   jQueryTag,
    //   jsTag("vendor/mousetrap.js"),
    //   jsAt("compiled/util.js"),
    //   jsAt("compiled/trans.js"),
    //   analyseTag,
    //   jsTag("embed-analyse.js"),
    //   embedJs(s"""lichess.startEmbeddedAnalyse({
    // element: document.querySelector('.embedded_study'),
    // study: ${safeJsonValue(data.study)},
    // data: ${safeJsonValue(data.analysis)},
    // embed: true,
    // i18n: ${views.html.board.userAnalysisI18n()},
    // userId: null
    // });""")
    // )
    )
  )

  def notFound()(implicit req: RequestHeader) = frag(
    doctype,
    htmlTag(lang)(
      topComment,
      head(
        charset,
        metaCsp(basicCsp),
        st.headTitle("404 - Study not available"),
        // fontStylesheets, // use proper stylesheets instead
        // currentBgCss,
        cssTags("common.css", "analyse-embed.css")
      ),
      body()(
        div(cls := "not-found")(
          h1("Study not available")
        )
      )
    )
  )
}
