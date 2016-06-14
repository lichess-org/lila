package lila.app
package templating

import controllers._
import play.api.i18n.{ Lang, Messages }
import play.api.libs.json.JsObject
import play.api.mvc.{ RequestHeader, Call }
import play.twirl.api.Html

import lila.i18n.Env.{ current => i18nEnv }
import lila.i18n.{ LangList, I18nDomain, I18nKey }
import lila.user.UserContext

trait I18nHelper {

  private def pool = i18nEnv.pool
  private def transInfos = i18nEnv.transInfos
  private def hideCallsCookieName = i18nEnv.hideCallsCookieName

  lazy val trans = i18nEnv.keys
  lazy val protocol = i18nEnv.RequestHandlerProtocol

  implicit def lang(implicit ctx: UserContext) = pool lang ctx.req

  def transKey(key: String, args: Seq[Any] = Nil)(implicit lang: Lang): String =
    i18nEnv.translator.transTo(key, args)(lang)

  def i18nJsObjectMessage(keys: Seq[I18nKey])(implicit lang: Lang): JsObject =
    i18nEnv.jsDump.keysToMessageObject(keys, lang)

  def i18nJsObject(keys: I18nKey*)(implicit lang: Lang): JsObject =
    i18nEnv.jsDump.keysToObject(keys, lang)

  def i18nOptionJsObject(keys: Option[I18nKey]*)(implicit lang: Lang): JsObject =
    i18nEnv.jsDump.keysToObject(keys.flatten, lang)

  def langName(lang: Lang): Option[String] = langName(lang.language)
  def langName(lang: String): Option[String] = LangList name lang

  def shortLangName(lang: Lang): Option[String] = shortLangName(lang.language)
  def shortLangName(lang: String): Option[String] = langName(lang) map (_ takeWhile (','!=))

  def translationCall(implicit ctx: UserContext) = i18nEnv.call(ctx.me, ctx.req)

  def transValidationPattern(trans: String) =
    (trans contains "%s") option ".*%s.*"

  private lazy val langAnnotationsBase: String =
    pool.names.keySet diff Set("fp", "kb", "le", "tp", "pi", "io") map { code =>
      s"""<link rel="alternate" hreflang="$code" href="//$code.lichess.org%"/>"""
    } mkString ""

  def langAnnotations(implicit ctx: UserContext) = Html {
    langAnnotationsBase.replace("%", ctx.req.uri)
  }

  def commonDomain(implicit ctx: UserContext): String =
    I18nDomain(ctx.req.domain).commonDomain

  def acceptLanguages(implicit ctx: UserContext): List[String] =
    ctx.req.acceptLanguages.map(_.language.toString).toList.distinct

  def acceptsLanguage(lang: Lang)(implicit ctx: UserContext): Boolean =
    ctx.req.acceptLanguages exists (_.language == lang.language)

  private val uriPlaceholder = "[URI]"

  private def langUrl(lang: Lang)(i18nDomain: I18nDomain) =
    protocol + (i18nDomain withLang lang).domain + uriPlaceholder
}
