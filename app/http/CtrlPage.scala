package lila.app
package http

import play.api.mvc.*
import scalatags.Text.Frag

import lila.ui.{ Page, RenderedPage, Snippet }

trait CtrlPage(using Executor) extends RequestContext with ControllerHelpers with lila.web.ResponseWriter:

  def renderPage(page: Page)(using Context): Fu[RenderedPage] =
    pageContext.map: pctx =>
      views.base.page(page)(using pctx)

  def renderAsync(page: Fu[Page])(using Context): Fu[RenderedPage] =
    pageContext.flatMap: pctx =>
      page.map(views.base.page(_)(using pctx))

  extension (s: Status)

    def page(page: Page)(using Context): Fu[Result] =
      pageContext.map: pctx =>
        s(views.base.page(page)(using pctx))

    def pageAsync(page: Fu[Page])(using Context): Fu[Result] =
      pageContext.flatMap: pctx =>
        page.map(views.base.page(_)(using pctx)).map(s(_))

    def snippetAsync(snippet: Fu[Frag])(using Context): Fu[Result] =
      snippet.dmap(Snippet(_)).map(s(_))

    def snippet(snippet: Frag)(using Context): Result =
      s(Snippet(snippet))

    // #TODO simplify
    def async(page: Fu[Page])(using Context): Fu[Result] = s.pageAsync(page)
