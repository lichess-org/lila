package lila.app
package templating

import scala.util.Random.shuffle

import controllers._
import play.api.i18n.Lang
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

  def i18nJsObject(keys: I18nKey*)(implicit lang: Lang) =
    i18nEnv.jsDump.keysToObject(keys, lang)

  def langName(lang: Lang): Option[String] = langName(lang.language)
  def langName(lang: String): Option[String] = LangList name lang

  def shortLangName(lang: Lang): Option[String] = shortLangName(lang.language)
  def shortLangName(lang: String): Option[String] = langName(lang) map (_ takeWhile (','!=))

  def translationCall(implicit ctx: UserContext) =
    if (ctx.isAnon || ctx.req.cookies.get(hideCallsCookieName).isDefined) None
    else (~ctx.me.map(_.count.game) >= 8000) ?? shuffle(
      (ctx.req.acceptLanguages map transInfos.get).flatten filter (_.nonComplete)
    ).headOption

  def transValidationPattern(trans: String) =
    (trans contains "%s") option ".*%s.*"

  def langFallbackLinks(implicit ctx: UserContext) = Html {
    pool.preferredNames(ctx.req, 3).map {
      case (code, name) => """<a class="lang_fallback" lang="%s" href="%s">%s</a>""".format(
        code, langUrl(Lang(code))(I18nDomain(ctx.req.domain)), name)
    }.mkString("").replace(uriPlaceholder, ctx.req.uri)
  }

  private lazy val langAnnotationsBase: String =
    pool.names.keySet diff Set("fp", "kb", "le", "tp", "pi", "io") map { code =>
      s"""<link rel="alternate" hreflang="$code" href="http://$code.lichess.org%"/>"""
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
