package views.html
package base

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object notFound {

  def apply()(implicit ctx: Context) = layout(
    title = "Page not found",
    moreJs = prismicJs,
    moreCss = responsiveCssTag("not-found"),
    responsive = true,
    csp = isGranted(_.Prismic) option defaultCsp.withPrismic(true)
  ) {
      main(cls := "not-found page-small box box-pad")(
        header(
          h1("404"),
          div(
            strong("Page not found!"),
            p(
              "Return to ",
              a(href := routes.Lobby.home)("the homepage")
            )
          )
        )
      )
    }
}
