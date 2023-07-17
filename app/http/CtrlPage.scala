package lila.app
package http

import play.api.libs.json.*
import play.api.http.*
import play.api.mvc.*
import scalatags.Text.Frag

trait CtrlPage(using Executor) extends RequestContext with ControllerHelpers with ResponseWriter:

  def renderPage(render: PageContext ?=> Frag)(using Context): Fu[Frag] =
    pageContext.map(render(using _))
  def renderAsync(render: PageContext ?=> Fu[Frag])(using Context): Fu[Frag] =
    pageContext.flatMap(render(using _))
  def withPageContext[A](render: PageContext ?=> A)(using Context): Fu[A] =
    pageContext.map(render(using _))

  extension (s: Status)
    def page(render: PageContext ?=> Frag)(using Context): Fu[Result] =
      pageContext.map(render(using _)).map(s(_))
    def pageAsync(render: PageContext ?=> Fu[Frag])(using Context): Fu[Result] =
      pageContext.flatMap(render(using _)).map(s(_))
    def async(render: Fu[Frag]) = render.map(s(_))
