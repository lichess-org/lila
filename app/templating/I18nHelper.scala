package lila.app
package templating

import play.api.i18n.Lang
import play.api.libs.json.JsObject
import play.api.mvc.Call

import lila.api.Context
import lila.app.ui.ScalatagsTemplate.*
import lila.i18n.{ JsDump, LangList, Translator }
import lila.hub.i18n.{ I18nKey, Translate }

trait I18nHelper:

  export LangList.{ nameByStr as langName }

  given (using ctx: Context): Lang    = ctx.lang
  given lila.hub.i18n.Translator      = lila.i18n.Translator
  given (using lang: Lang): Translate = lila.i18n.Translator.to(lang)

  def transKey(key: I18nKey, args: Seq[Matchable] = Nil)(using lang: Lang): Frag =
    Translator.frag.literal(key, args, lang)

  def i18nJsObject(keys: Seq[I18nKey])(using Lang): JsObject =
    JsDump.keysToObject(keys)

  def i18nOptionJsObject(keys: Option[I18nKey]*)(using Lang): JsObject =
    JsDump.keysToObject(keys.flatten)

  def shortLangName(str: String) = langName(str).takeWhile(','.!=)

  def isRTL(using lang: Lang) = lila.i18n.LangList.isRTL(lang)

  def langHref(call: Call)(using Context): String = langHref(call.url)
  def langHref(path: String)(using ctx: Context): String =
    if ctx.isAuth || ctx.lang.language == "en"
    then path
    else
      val code = lila.i18n.fixJavaLanguage(ctx.lang)
      if path == "/" then s"/$code"
      else s"/$code$path"
