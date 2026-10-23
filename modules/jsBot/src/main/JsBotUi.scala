package lila.jsBot
package ui

import play.api.libs.json.{ JsObject, Json }

import lila.common.Json.given
import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }

final class JsBotUi(helpers: Helpers):
  import helpers.*

  def play(
      bots: List[BotJson],
      prefs: JsObject
  )(using Option[Me]): Page =
    val data = Json
      .obj("pref" -> prefs, "bots" -> bots)
      .add("devBots" -> Option.when(Granter.opt(_.BotEditor))(lila.jsBot.devBotKeys))
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
    val moduleName = "botDev"
    Page("Lichess bots")
      .css("botDev")
      .css("round")
      .css(ctx.pref.hasKeyboardMove.option("keyboardMove"))
      .css(ctx.pref.hasVoice.option("voice"))
      .js(PageModule(moduleName, data))
      .js(Esm("round"))
      .i18nOpt(ctx.pref.hasVoice, _.voiceCommands)
      .csp(_.withWebAssembly)
      .flag(_.zoom):
        main(cls := "bot-play-app")
