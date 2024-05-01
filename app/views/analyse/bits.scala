package views.analyse

import lila.app.templating.Environment.{ *, given }

import play.api.libs.json.{ Json, JsObject }

object bits:

  val dataPanel = attr("data-panel")

  def page(title: String)(using Context): Page =
    Page(title).zoom.robots(false).csp(csp)

  def csp(using Context): Update[lila.ui.ContentSecurityPolicy] =
    analysisCsp.compose(_.withPeer.withInlineIconFont.withChessDbCn)

  def analyseModule(mode: String, json: JsObject)(using ctx: PageContext) =
    PageModule("analyse", Json.obj("mode" -> mode, "cfg" -> json))
