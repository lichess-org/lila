package lila.ui

import play.api.i18n.Lang
import play.api.libs.json.JsObject

import lila.core.i18n.{ I18nKey, JsDump, LangList, Translator, fixJavaLanguage }
import lila.ui.ScalatagsTemplate.*

trait I18nHelper:

  protected val jsDump: JsDump
  protected val translator: Translator
  protected val ratingApi: lila.ui.RatingApi

  val langList: LangList

  extension (pk: PerfKey)
    def perfIcon: Icon                                = ratingApi.toIcon(pk)
    def perfName: I18nKey                             = ratingApi.toNameKey(pk)
    def perfDesc: I18nKey                             = ratingApi.toDescKey(pk)
    def perfTrans(using translate: Translate): String = perfName.txt()

  export lila.core.i18n.Translate
  export lila.core.i18n.I18nKey as trans
  export I18nKey.{ txt, pluralTxt, pluralSameTxt, apply, plural, pluralSame }

  given ctxTrans(using ctx: Context): Translate = Translate(translator, ctx.lang)
  given transLang(using trans: Translate): Lang = trans.lang

  def transDefault: Translate = translator.toDefault

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
