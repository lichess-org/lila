package lila.app
package http

import play.api.mvc.*
import scalatags.Text.all.Frag

import lila.ui.{ Page, Snippet }

trait CtrlPage(using Executor) extends RequestContext with ControllerHelpers with lila.web.ResponseWriter:

  def renderPage(render: Context ?=> Page)(using Context): Fu[Frag] =
    pageContext.map: pctx =>
      given PageContext = pctx
      views.base.page(render)

  def renderAsync(render: Context ?=> Fu[Page])(using Context): Fu[Frag] =
    pageContext.flatMap: pctx =>
      given PageContext = pctx
      render.map(views.base.page)

  def withPageContext[A](render: PageContext ?=> A)(using Context): Fu[A] =
    pageContext.map(render(using _))

  extension (s: Status)

    def page(render: Context ?=> Page)(using Context): Fu[Result] =
      renderPage(render).map(s(_))

    def pageAsync(render: Context ?=> Fu[Page])(using Context): Fu[Result] =
      pageContext.flatMap: pctx =>
        given PageContext = pctx
        render.map(views.base.page).map(s(_))

    def snippetAsync(render: Context ?=> Fu[Snippet])(using Context): Fu[Result] =
      render.map(s(_))

    // #TODO simplify
    def async(render: Context ?=> Fu[Page])(using Context): Fu[Result] = s.pageAsync(render)
