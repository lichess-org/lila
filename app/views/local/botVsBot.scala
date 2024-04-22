package views.html.local

import controllers.routes
import play.api.libs.json.{ JsObject, Json }

import lila.app.templating.Environment.{ given, * }
import lila.ui.ScalatagsTemplate.*
import lila.common.Json.given
import lila.common.String.html.safeJsonValue

object botVsBot:
  def index(using ctx: PageContext) =
    views.html.base.layout(
      title = "Play vs Bots",
      modules = jsModuleInit("local.botVsBot"),
      moreCss = cssTag("bot-vs-bot"),
      csp = analysisCsp.some,
      openGraph = lila.web
        .OpenGraph(
          title = "Bots vs Bots",
          description = "Bots vs Bots",
          url = s"$netBaseUrl${controllers.routes.Local.botVsBot}"
        )
        .some,
      zoomable = true
    ) {
      main
    }
