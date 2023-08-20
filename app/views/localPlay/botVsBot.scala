package views.html.localPlay

import controllers.routes
import play.api.libs.json.{ JsObject, Json }

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import lila.common.Json.given
import lila.common.String.html.safeJsonValue

object botVsBot:
  def index(using ctx: PageContext) =
    views.html.base.layout(
      title = "Play vs Bots",
      moreJs = jsModuleInit("localPlay", Json.obj("mode" -> "botVsBot")),
      moreCss = cssTag("bot-vs-bot"),
      csp = analysisCsp.some,
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Bots vs Bots",
          description = "Bots vs Bots",
          url = s"$netBaseUrl${controllers.routes.LocalPlay.botVsBot}"
        )
        .some,
      zoomable = true
    ) {
      main
    }
