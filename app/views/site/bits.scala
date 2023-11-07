package views.html.site

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import controllers.routes

object bits:

  def getFishnet()(using PageContext) =
    views.html.base.layout(
      title = "fishnet API key request",
      csp = defaultCsp.withGoogleForm.some
    ) {
      main(
        iframe(
          src := "https://docs.google.com/forms/d/e/1FAIpQLSeGgDHgWGP0uobQknF92eCMXqebyNBTyzJoJqbeGjRezlbWOw/viewform?embedded=true",
          style          := "width:100%;height:1400px",
          st.frameborder := 0,
          frame.credentialless
        )(spinner)
      )
    }

  def api = raw:
    """<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Security-Policy" content="default-src 'self'; style-src 'unsafe-inline'; script-src https://cdn.jsdelivr.net blob:; child-src blob:; connect-src https://raw.githubusercontent.com; img-src data: https://lichess.org https://lichess1.org;">
    <title>Lichess.org API reference</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>body { margin: 0; padding: 0; }</style>
  </head>
  <body>
    <redoc spec-url="https://raw.githubusercontent.com/lichess-org/api/master/doc/specs/lichess-api.yaml"></redoc>
    <script src="https://cdn.jsdelivr.net/npm/redoc@next/bundles/redoc.standalone.js"></script>
  </body>
</html>"""

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

  def subnav(mods: Modifier*) = st.aside(cls := "subnav"):
    st.nav(cls := "subnav__inner")(mods)

  def pageMenuSubnav(mods: Modifier*) = subnav(cls := "page-menu__menu", mods)
