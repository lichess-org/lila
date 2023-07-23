package views.html

import controllers.routes
import play.api.libs.json.{ JsObject, Json }

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import lila.common.Json.given
import lila.common.String.html.safeJsonValue

object localPlay:
  def index(using ctx: PageContext) =
    views.html.base.layout(
      title = "Play vs Bots",
      moreJs = jsModuleInit("localPlay"),
      moreCss = cssTag("local-play"),
      csp = analysisCsp.some,
      openGraph = lila.app.ui
        .OpenGraph(
          title = "Play vs Bots",
          description = "Play vs Bots",
          url = s"$netBaseUrl${controllers.routes.LocalPlay.index}"
        )
        .some,
      chessground = false,
      withHrefLangs = lila.common.LangPath(controllers.routes.LocalPlay.index).some
    ) {
      main(id := "local-play")(
        div
      )
    }
