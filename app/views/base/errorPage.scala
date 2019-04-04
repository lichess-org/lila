package views.html
package base

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object errorPage {

  def apply(ex: Throwable)(implicit ctx: Context) = {
    layout(
      title = "Internal server error",
      responsive = true
    ) {
      main(cls := "page-small box box-pad")(
        h1("Something went wrong on this page"),
        p(
          "If the problem persists, please report it in the ",
          a(href := routes.ForumCateg.show("lidraughts-feedback", 1))("forum"),
          "."
        ),
        p(
          "Or send us an email at ",
          contactEmail,
          "."
        ),
        code(ex.getMessage)
      )
    }
  }.toHtml
}
