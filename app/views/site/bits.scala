package views.html.site

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import controllers.routes

object bits {

  def api =
    raw(
      s"""<!DOCTYPE html>
<html>
  <head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Security-Policy" content="default-src 'self'; style-src fonts.googleapis.com 'unsafe-inline'; font-src fonts.gstatic.com; script-src 'unsafe-eval' https://cdn.jsdelivr.net blob:; child-src blob:; connect-src ${env.net.assetDomain}; img-src data: https://lishogi.org https://lishogi1.org;">
    <title>Lishogi.org API reference - WIP</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <link href="https://fonts.googleapis.com/css?family=Montserrat:300,400,700|Roboto:300,400,700" rel="stylesheet">
    <style>body { margin: 0; padding: 0; }</style>
  </head>
  <body>
    <redoc spec-url="//${env.net.assetDomain}/assets/doc/lishogi-api.yaml"></redoc>
    <script src="https://cdn.jsdelivr.net/npm/redoc@next/bundles/redoc.standalone.js"></script>
  </body>
</html>"""
    )

  def errorPage(implicit ctx: Context) =
    views.html.base.layout(
      title = "Internal server error"
    ) {
      main(cls := "page-small box box-pad")(
        h1("Something went wrong on this page"),
        p(
          "If the problem persists, please ",
          a(href := s"${routes.Main.contact}#help-error-page")("report the bug"),
          "."
        )
      )
    }
}
