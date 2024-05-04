package views.local

import play.api.libs.json.{ JsObject, Json }

import lila.app.UiEnv.{ *, given }
import lila.common.Json.given
import lila.common.String.html.safeJsonValue

object botVsBot:
  def index(using ctx: Context) =
    Page("")
      .copy(fullTitle = s"$siteName • Play vs Bots".some)
      .js(jsModuleInit("local.botVsBot"))
      .cssTag("bot-vs-bot")
      .csp(_.withWebAssembly)
      .graph(
        OpenGraph(
          title = "Bots vs Bots",
          description = "Bots vs Bots",
          url = netBaseUrl.value
        )
      )
      .hrefLangs(lila.ui.LangPath("/")) {
        main
      }
