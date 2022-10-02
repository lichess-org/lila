package lila.app
package templating

import play.api.i18n.Lang
import play.api.libs.json.JsObject
import play.api.mvc.Call

import lila.app.ui.ScalatagsTemplate._
import lila.i18n.{ I18nKey, JsDump, LangList, MessageKey, Translator }
import lila.user.UserContext

trait I18nHelper extends HasEnv with UserContext.ToLang {

  def transKey(key: MessageKey, args: Seq[Any] = Nil)(implicit lang: Lang): Frag =
    Translator.frag.literal(key, args, lang)

  def i18nJsObject(keys: Seq[MessageKey])(implicit lang: Lang): JsObject =
    JsDump.keysToObject(keys, lang)

  def i18nOptionJsObject(keys: Option[I18nKey]*)(implicit lang: Lang): JsObject =
    JsDump.keysToObject(keys.collect { case Some(k) => k.key }, lang)

  def langName = LangList.nameByStr _

  def shortLangName(str: String) = langName(str).takeWhile(','.!=)

  def isRTL(implicit lang: Lang) = lila.i18n.LangList.isRTL(lang)

  def langHref(call: Call)(implicit ctx: lila.api.Context): String = langHref(call.url)
  def langHref(path: String)(implicit ctx: lila.api.Context): String =
    if (ctx.isAuth || ctx.lang.language == "en") path
    else {
      val code = lila.i18n.fixJavaLanguageCode(ctx.lang)
      if (path == "/") s"/$code"
      else s"/$code$path"
    }
}
