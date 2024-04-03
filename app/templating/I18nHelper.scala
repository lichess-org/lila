package lila.app
package templating

import play.api.i18n.Lang
import play.api.libs.json.JsObject
import play.api.mvc.Call

import lila.api.Context
import lila.app.ui.ScalatagsTemplate.*
import lila.i18n.{ JsDump, LangList, Translator }
import lila.core.i18n.{ I18nKey, Translate }

trait I18nHelper:

  export lila.core.i18n.Translate
  export lila.core.i18n.I18nKey as trans
  export LangList.{ nameByStr as langName }
  export I18nKey.{ txt, pluralTxt, pluralSameTxt, apply, plural, pluralSame }

  // given ctxLang(using ctx: Context): Lang       = ctx.lang
  given ctxTrans(using ctx: Context): Translate = ctx.translate
  // given lila.core.i18n.Translator       = lila.i18n.Translator
  // given (using lang: Lang): Translate           = lila.i18n.Translator.to(lang)
  given transLang(using trans: Translate): Lang = trans.lang

  def transKey(key: I18nKey, args: Seq[Matchable] = Nil)(using t: Translate): Frag =
    Translator.frag.literal(key, args, t.lang)

  def i18nJsObject(keys: Seq[I18nKey])(using Translate): JsObject =
    JsDump.keysToObject(keys)

  def i18nOptionJsObject(keys: Option[I18nKey]*)(using Translate): JsObject =
    JsDump.keysToObject(keys.flatten)

  def shortLangName(str: String) = langName(str).takeWhile(','.!=)

  def langHref(call: Call)(using Context): String = langHref(call.url)
  def langHref(path: String)(using ctx: Context): String =
    if ctx.isAuth || ctx.lang.language == "en"
    then path
    else
      val code = lila.i18n.fixJavaLanguage(ctx.lang)
      if path == "/" then s"/$code"
      else s"/$code$path"
