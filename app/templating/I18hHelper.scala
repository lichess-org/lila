package lila.app
package templating

import scala.collection.breakOut

import play.api.i18n.Lang
import play.api.libs.json.JsObject
import play.twirl.api.Html

import lila.i18n.Env.{ current => i18nEnv }
import lila.i18n.{ LangList, I18nKey, Translator }
import lila.user.UserContext

trait I18nHelper {

  implicit def lang(implicit ctx: UserContext) = ctx.lang

  def transKey(key: String, args: Seq[Any] = Nil)(implicit lang: Lang) =
    Translator.html.literal(key, args, lang)

  def i18nJsObjectMessage(keys: Seq[I18nKey])(implicit lang: Lang): JsObject =
    i18nEnv.jsDump.keysToMessageObject(keys, lang)

  def i18nJsObject(keys: I18nKey*)(implicit lang: Lang): JsObject =
    i18nEnv.jsDump.keysToObject(keys, lang)

  def i18nOptionJsObject(keys: Option[I18nKey]*)(implicit lang: Lang): JsObject =
    i18nEnv.jsDump.keysToObject(keys.flatten, lang)

  def langName = LangList.nameByStr _

  def shortLangName(str: String) = langName(str).takeWhile(','!=)
}
