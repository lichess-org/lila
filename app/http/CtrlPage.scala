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

    def page(page: Page)(using Context): Fu[Result]      = renderPage(page).map(s(_))
    def async(page: Fu[Page])(using Context): Fu[Result] = renderAsync(page).map(s(_))

    def snipAsync(frag: Fu[Frag | Snippet])(using Context): Fu[Result] = frag.dmap(snip)
    def snip(frag: Frag | Snippet)(using Context): Result = s(frag.match
      case s: Snippet => s
      case f: Frag    => Snippet(f))
