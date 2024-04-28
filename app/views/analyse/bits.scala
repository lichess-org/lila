package views.analyse

import lila.app.templating.Environment.{ *, given }

import play.api.libs.json.{ Json, JsObject }

object bits:

  val dataPanel = attr("data-panel")

  def layout(
      title: String,
      pageModule: PageModule,
      moreCss: Frag = emptyFrag,
      moreJs: Frag = emptyFrag,
      modules: EsmList = Nil,
      openGraph: Option[OpenGraph] = None
  )(body: Frag)(using PageContext): Frag =
    views.base.layout(
      title = title,
      moreCss = moreCss,
      moreJs = moreJs,
      modules = modules,
      pageModule = pageModule.some,
      openGraph = openGraph,
      robots = false,
      zoomable = true,
      csp = csp
    )(body)

  def csp(using PageContext) = analysisCsp.withPeer.withInlineIconFont.withChessDbCn.some

  def analyseModule(mode: String, json: JsObject)(using ctx: PageContext) =
    PageModule("analyse", Json.obj("mode" -> mode, "cfg" -> json))
