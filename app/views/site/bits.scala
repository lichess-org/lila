package views.html.site

import controllers.routes

import lila.app.templating.Environment.{ *, given }
import lila.web.ui.ScalatagsTemplate.{ *, given }

object bits:

  def getFishnet()(using PageContext) =
    views.html.base.layout(
      title = "fishnet API key request",
      csp = defaultCsp.withGoogleForm.some
    ):
      main:
        iframe(
          src := "https://docs.google.com/forms/d/e/1FAIpQLSeGgDHgWGP0uobQknF92eCMXqebyNBTyzJoJqbeGjRezlbWOw/viewform?embedded=true",
          style          := "width:100%;height:1400px",
          st.frameborder := 0,
          frame.credentialless
        )(spinner)

  def errorPage(using PageContext) =
    views.html.base.layout(title = "Internal server error"):
      main(cls := "page-small box box-pad")(
        h1(cls := "box__top")("Something went wrong on this page"),
        p(
          "If the problem persists, please ",
          a(href := s"${routes.Main.contact}#help-error-page")("report the bug"),
          "."
        )
      )

  def ghost(using PageContext) =
    views.html.base.layout(
      moreCss = cssTag("ghost"),
      title = "Deleted user"
    ):
      main(cls := "page-small box box-pad page")(
        h1(cls := "box__top")("Deleted user"),
        div(
          p("This player account is gone!"),
          p("Nothing to see here, move along.")
        )
      )
