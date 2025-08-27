package lila.jsBot
package ui

import play.api.libs.json.{ Json, JsObject }

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.common.Json.given

final class JsBotUi(helpers: Helpers):
  import helpers.*

  def play(
      bots: List[BotJson],
      prefs: JsObject
  ): Page =
    val data = Json.obj("pref" -> prefs, "bots" -> bots)
    Page("Lichess bots")
      .css("botPlay")
      .js(PageModule("botPlay.main", data))
      .flag(_.zen)
      .flag(_.playing)
      .csp(_.withWebAssembly)
      .flag(_.zoom):
        main(cls := "bot-play")

  // def testPlay(prefJson: JsObject)(using ctx: Context): Page =
  //   Page("Lichess bots")
  //     .css("botDev")
  //     .css("round")
  //     .css(ctx.pref.hasKeyboardMove.option("keyboardMove"))
  //     .css(ctx.pref.hasVoice.option("voice"))
  //     .js(lila.ui.PageModule("botDev.user", Json.obj("pref" -> prefJson)))
  //     .js(lila.ui.Esm("round"))
  //     .flag(_.playing)
  //     .csp(_.withWebAssembly)
  //     .flag(_.zoom)(main)

  def dev(
      bots: List[BotJson],
      prefs: JsObject,
      devAssets: JsObject
  )(using ctx: Context): Page =
    val data = Json
      .obj("pref" -> prefs, "bots" -> bots, "assets" -> devAssets)
      .add("canPost", Granter.opt(_.BotEditor))
    val moduleName = "botDev"
    Page("Lichess bots")
      .css("botDev")
      .css("round")
      .css(ctx.pref.hasKeyboardMove.option("keyboardMove"))
      .css(ctx.pref.hasVoice.option("voice"))
      .js(PageModule(moduleName, data))
      .js(Esm("round"))
      .csp(_.withWebAssembly)
      .flag(_.zoom):
        main(cls := "bot-play-app")
