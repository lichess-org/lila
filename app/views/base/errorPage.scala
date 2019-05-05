package views.html
package base

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object errorPage {

  def apply(ex: Throwable)(implicit ctx: Context) = layout(
    title = "Internal server error"
  ) {
    main(cls := "page-small box box-pad")(
      h1("Something went wrong on this page"),
      p(
        "If the problem persists, please ",
        a(href := s"${routes.Main.contact}#help-error-page")("report the bug"),
        "."
      ),
      code(ex.getMessage)
    )
  }
}
