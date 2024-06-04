package lila.app
package templating

import play.api.i18n.Lang
import play.api.libs.json.JsObject
import play.api.mvc.Call

import lila.app.ui.ScalatagsTemplate._
import lila.i18n.{ I18nKey, I18nKeys => trans, JsDump, LangList, MessageKey, TimeagoLocales, Translator }
import lila.user.UserContext

trait I18nHelper extends HasEnv with UserContext.ToLang {

  def transKey(key: MessageKey, args: Seq[Any] = Nil)(implicit lang: Lang): Frag =
    Translator.frag.literal(key, args, lang)

  def transKeyTxt(key: MessageKey, args: Seq[Any] = Nil)(implicit lang: Lang): String =
    Translator.txt.literal(key, args, lang)

  def i18nJsObject(keys: Seq[MessageKey])(implicit lang: Lang): JsObject =
    JsDump.keysToObject(keys, lang)

  def i18nOptionJsObject(keys: Option[I18nKey]*)(implicit lang: Lang): JsObject =
    JsDump.keysToObject(keys.collect { case Some(k) => k.key }, lang)

  def timeagoLocaleScript(implicit ctx: lila.api.Context): String = {
    TimeagoLocales.js.get(ctx.lang.code) orElse
      TimeagoLocales.js.get(ctx.lang.language) getOrElse
      ~TimeagoLocales.js.get("en")
  }

  def langName = LangList.nameByStr _

  def shortLangName(str: String) = langName(str).takeWhile(','.!=)

  def langHref(call: Call)(implicit ctx: lila.api.Context): String = langHref(call.url)
  def langHref(url: String)(implicit ctx: lila.api.Context): String =
    if (ctx.isAuth || ctx.lang.language == "en") url
    else urlWithLangQuery(url, lila.i18n.languageCode(ctx.lang))

  def langHrefJP(call: Call)(implicit ctx: lila.api.Context): String =
    langHrefJP(call.url)
  def langHrefJP(url: String)(implicit ctx: lila.api.Context): String =
    if (ctx.isAuth || ctx.lang.language != "ja") url
    else urlWithLangQuery(url, "ja")

  def urlWithLangQuery(url: String, langCode: String): String =
    s"$url${if (url.contains("?")) "&" else "?"}lang=$langCode"

  val nvuiTranslations = Vector[I18nKey](
    trans.nvui.textualRepresentation,
    trans.nvui.gameInfo,
    trans.nvui.pieces,
    trans.nvui.board,
    trans.nvui.hands,
    trans.nvui.none,
    trans.nvui.moves,
    trans.nvui.currentPosition,
    trans.nvui.moveForm,
    trans.nvui.commandInput,
    trans.nvui.commands,
    trans.nvui.useArrowKeys,
    trans.rated,
    trans.casual,
    trans.clock,
    trans.computerAnalysis,
    trans.notationSystem,
    trans.settings.settings
  ).map(_.key)

}
