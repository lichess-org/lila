package lila.local
package ui

import play.api.libs.json.{ Json, JsObject }

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.common.Json.{ *, given }

final class LocalUi(helpers: Helpers):
  import helpers.{ *, given }

  def index(
      bots: List[BotJson],
      prefs: JsObject,
      devAssets: Option[JsObject] = none
  )(using ctx: Context): Page =
    val data = Json
      .obj("pref" -> prefs, "bots" -> bots)
      .add("assets", devAssets)
      .add("canPost", Granter.opt(_.BotEditor))
    val moduleName = if devAssets.isDefined then "local.dev" else "botPlay"
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
