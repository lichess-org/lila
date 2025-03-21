package lila.local
package ui

import play.api.libs.json.{ Json, JsObject }

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.common.Json.{ *, given }

final class LocalUi(helpers: Helpers):
  import helpers.{ *, given }

  def play(
      bots: List[BotJson],
      prefs: JsObject
  )(using ctx: Context): Page =
    val data = Json.obj("pref" -> prefs, "bots" -> bots)
    Page("Lichess bots")
      .css("botPlay")
      .js(PageModule("botPlay.main", data))
      .flag(_.zen)
      .flag(_.playing)
      .csp(_.withWebAssembly)
      .flag(_.zoom):
        main(cls := "bot-play")

  def dev(
      bots: List[BotJson],
      prefs: JsObject,
      devAssets: JsObject
  )(using ctx: Context): Page =
    val data = Json
      .obj("pref" -> prefs, "bots" -> bots, "assets" -> devAssets)
      .add("canPost", Granter.opt(_.BotEditor))
    val moduleName = "local.dev"
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
