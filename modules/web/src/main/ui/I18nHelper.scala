package lila.web
package ui

import play.api.i18n.Lang
import play.api.libs.json.JsObject
import play.api.mvc.Call

import lila.web.ui.ScalatagsTemplate.*
import lila.core.i18n.{ I18nKey, fixJavaLanguage }

trait I18nHelper:

  def jsDump: lila.core.i18n.JsDump
  def translator: lila.core.i18n.Translator

  export lila.core.i18n.Translate
  export lila.core.i18n.I18nKey as trans
  export I18nKey.{ txt, pluralTxt, pluralSameTxt, apply, plural, pluralSame }

  given ctxTrans(using ctx: Context): Translate = Translate(translator, ctx.lang)
  given transLang(using trans: Translate): Lang = trans.lang

  def transKey(key: I18nKey, args: Seq[Matchable] = Nil)(using t: Translate): Frag =
    translator.frag.literal(key, args, t.lang)

  def i18nJsObject(keys: Seq[I18nKey])(using Translate): JsObject =
    jsDump.keysToObject(keys)

  def i18nOptionJsObject(keys: Option[I18nKey]*)(using Translate): JsObject =
    jsDump.keysToObject(keys.flatten)

  def langHref(call: Call)(using Context): String = langHref(call.url)
  def langHref(path: String)(using ctx: Context): String =
    if ctx.isAuth || ctx.lang.language == "en"
    then path
    else
      val code = fixJavaLanguage(ctx.lang)
      if path == "/" then s"/$code"
      else s"/$code$path"
