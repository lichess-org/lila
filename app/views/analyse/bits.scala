package views.html.analyse

import lila.app.templating.Environment.*
import lila.app.ui.ScalatagsTemplate.*
import play.api.libs.json.{ Json, JsObject }

object bits:

  val dataPanel = attr("data-panel")

  def layout(
      title: String,
      pageModule: PageModule,
      moreCss: Frag = emptyFrag,
      moreJs: Frag = emptyFrag,
      openGraph: Option[lila.app.ui.OpenGraph] = None
  )(body: Frag)(using PageContext): Frag =
    views.html.base.layout(
      title = title,
      moreCss = moreCss,
      moreJs = moreJs,
      pageModule = pageModule.some,
      openGraph = openGraph,
      robots = false,
      zoomable = true,
      csp = csp
    )(body)

  def csp(using PageContext) = analysisCsp.withPeer.withInlineIconFont.withChessDbCn.some

  def analyseModule(mode: String, json: JsObject)(using ctx: PageContext) =
    PageModule("analysisBoard", Json.obj("mode" -> mode, "cfg" -> json))
