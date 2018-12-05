package lila.app
package templating

import play.api.i18n.Lang
import play.api.libs.json.JsObject
import play.twirl.api.Html

import lila.i18n.{ LangList, I18nKey, Translator, JsQuantity, I18nDb, JsDump, TimeagoLocales }
import lila.user.UserContext

trait I18nHelper {

  implicit def lang(implicit ctx: UserContext) = ctx.lang

  def transKey(key: String, db: I18nDb.Ref, args: Seq[Any] = Nil)(implicit lang: Lang): Html =
    Translator.html.literal(key, db, args, lang)

  def i18nJsObject(keys: Seq[I18nKey])(lang: Lang): JsObject =
    JsDump.keysToObject(keys, I18nDb.Site, lang)

  def i18nOptionJsObject(keys: Option[I18nKey]*)(implicit lang: Lang): JsObject =
    JsDump.keysToObject(keys.flatten, I18nDb.Site, lang)

  def i18nFullDbJsObject(db: I18nDb.Ref)(implicit lang: Lang): JsObject =
    JsDump.dbToObject(db, lang)

  private val defaultTimeagoLocale = TimeagoLocales.js.get("en") err "Missing en TimeagoLocales"
  def timeagoLocaleScript(implicit ctx: lila.api.Context): String = {
    TimeagoLocales.js.get(ctx.lang.code) orElse
      TimeagoLocales.js.get(ctx.lang.language) getOrElse
      defaultTimeagoLocale
  }

  def langName = LangList.nameByStr _

  def shortLangName(str: String) = langName(str).takeWhile(','!=)
}
