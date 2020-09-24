package lila.app
package templating

import play.api.libs.json.JsObject
import play.api.i18n.Lang

import lila.app.ui.ScalatagsTemplate._
import lila.i18n.{ I18nKey, JsDump, LangList, MessageKey, TimeagoLocales, Translator }
import lila.user.UserContext

trait I18nHelper extends HasEnv with UserContext.ToLang {

  def transKey(key: MessageKey, args: Seq[Any] = Nil)(implicit lang: Lang): Frag =
    Translator.frag.literal(key, args, lang)

  def i18nJsObject(keys: Seq[MessageKey])(implicit lang: Lang): JsObject =
    JsDump.keysToObject(keys, lang)

  def i18nOptionJsObject(keys: Option[I18nKey]*)(implicit lang: Lang): JsObject =
    JsDump.keysToObject(keys.collect { case Some(k) => k.key }, lang)

  def timeagoLocaleScript(implicit lang: Lang): String = {
    TimeagoLocales.js.get(lang.code) orElse
      TimeagoLocales.js.get(lang.language) getOrElse
      ~TimeagoLocales.js.get("en")
  }

  def langName = LangList.nameByStr _

  def shortLangName(str: String) = langName(str).takeWhile(','.!=)
}
