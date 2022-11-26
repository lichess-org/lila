package lila.app
package templating

import play.api.i18n.Lang
import play.api.libs.json.JsObject
import play.api.mvc.Call

import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.i18n.{ I18nKey, JsDump, LangList, MessageKey, Translator }
import lila.user.UserContext

trait I18nHelper:

  export LangList.{ nameByStr as langName }

  def transKey(key: MessageKey, args: Seq[Matchable] = Nil)(using lang: Lang): Frag =
    Translator.frag.literal(key, args, lang)

  def i18nJsObject(keys: Seq[MessageKey])(using lang: Lang): JsObject =
    JsDump.keysToObject(keys, lang)

  def i18nOptionJsObject(keys: Option[I18nKey]*)(using lang: Lang): JsObject =
    JsDump.keysToObject(keys.collect { case Some(k) => k.key }, lang)

  def shortLangName(str: String) = langName(str).takeWhile(','.!=)

  def isRTL(using lang: Lang) = lila.i18n.LangList.isRTL(lang)

  def langHref(call: Call)(using ctx: lila.api.Context): String = langHref(call.url)
  def langHref(path: String)(using ctx: lila.api.Context): String =
    if (ctx.isAuth || ctx.lang.language == "en") path
    else
      val code = lila.i18n.fixJavaLanguageCode(ctx.lang)
      if (path == "/") s"/$code"
      else s"/$code$path"
