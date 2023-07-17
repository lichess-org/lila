package lila.app
package templating

import play.api.i18n.Lang
import play.api.libs.json.JsObject
import play.api.mvc.Call

import lila.app.ui.ScalatagsTemplate.*
import lila.i18n.{ I18nKey, JsDump, LangList, Translator }
import lila.api.Context

trait I18nHelper:

  export LangList.{ nameByStr as langName }

  given (using ctx: Context): Lang = ctx.lang

  def transKey(key: I18nKey, args: Seq[Matchable] = Nil)(using lang: Lang): Frag =
    Translator.frag.literal(key, args, lang)

  def i18nJsObject(keys: Seq[I18nKey])(using lang: Lang): JsObject =
    JsDump.keysToObject(keys, lang)

  def i18nOptionJsObject(keys: Option[I18nKey]*)(using lang: Lang): JsObject =
    JsDump.keysToObject(keys.flatten, lang)

  def shortLangName(str: String) = langName(str).takeWhile(','.!=)

  def isRTL(using lang: Lang) = lila.i18n.LangList.isRTL(lang)

  def langHref(call: Call)(using Context): String = langHref(call.url)
  def langHref(path: String)(using ctx: Context): String =
    if ctx.isAuth || ctx.lang.language == "en"
    then path
    else
      val code = lila.i18n.fixJavaLanguageCode(ctx.lang)
      if path == "/" then s"/$code"
      else s"/$code$path"
