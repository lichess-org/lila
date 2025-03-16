package lila.local
package ui

import play.api.libs.json.JsObject

import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class LocalUi(helpers: Helpers):
  import helpers.{ *, given }

  def index(data: JsObject, moduleName: "botPlay" | "local.dev" = "botPlay")(using ctx: Context): Page =
    Page("Lichess bots")
      .css(moduleName)
      .css("round")
      .css(ctx.pref.hasKeyboardMove.option("keyboardMove"))
      .css(ctx.pref.hasVoice.option("voice"))
      .js(PageModule(moduleName, data))
      .js(Esm("round"))
      .csp(_.withWebAssembly)
      .flag(_.zoom):
        main(cls := "bot-play-app")
