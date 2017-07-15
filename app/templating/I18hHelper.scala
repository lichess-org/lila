package lila.app
package templating

import scala.collection.breakOut

import play.api.i18n.Lang
import play.api.libs.json.JsObject
import play.twirl.api.Html

import lila.i18n.Env.{ current => i18nEnv }
import lila.i18n.{ LangList, I18nKey, Translator, JsQuantity, I18nDb, JsDump }
import lila.user.UserContext

trait I18nHelper {

  implicit def lang(implicit ctx: UserContext) = ctx.lang

  def transKey(key: String, db: I18nDb.Ref, args: Seq[Any] = Nil)(implicit lang: Lang): Html =
    Translator.html.literal(key, db, args, lang)

  def i18nJsObject(keys: I18nKey*)(implicit lang: Lang): JsObject =
    JsDump.keysToObject(keys, lang)

  def i18nOptionJsObject(keys: Option[I18nKey]*)(implicit lang: Lang): JsObject =
    JsDump.keysToObject(keys.flatten, lang)

  def i18nJsQuantityFunction()(implicit lang: Lang): Html = Html(JsQuantity(lang))

  def langName = LangList.nameByStr _

  def shortLangName(str: String) = langName(str).takeWhile(','!=)
}
