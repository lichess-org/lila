package lila.app
package templating

import controllers._
import lila.user.Context
import lila.i18n.Env.{ current ⇒ i18nEnv }
import lila.i18n.{ LangList, I18nDomain }

import play.api.i18n.Lang
import play.api.templates.Html
import play.api.mvc.{ RequestHeader, Call }
import scala.util.Random.shuffle

trait I18nHelper {

  private def pool = i18nEnv.pool
  private def transInfos = i18nEnv.transInfos
  private def hideCallsCookieName = i18nEnv.hideCallsCookieName

  lazy val trans = i18nEnv.keys
  lazy val protocol = i18nEnv.RequestHandlerProtocol

  implicit def lang(implicit ctx: Context) = pool lang ctx.req

  def langName(lang: Lang) = LangList name lang.language

  def translationCall(implicit ctx: Context) =
    if (ctx.req.cookies.get(hideCallsCookieName).isDefined) None
    else shuffle(
      (ctx.req.acceptLanguages map transInfos.get).flatten filter (_.nonComplete)
    ).headOption

  def transValidationPattern(trans: String) =
    (trans contains "%s") option ".*%s.*"

  private lazy val langLinksCache =
    scala.collection.mutable.Map[String, String]()

  def langLinks(lang: Lang)(implicit ctx: Context) = Html {
    langLinksCache.getOrElseUpdate(lang.language, {
      val i18nDomain = I18nDomain(ctx.req.domain)
      pool.names.toList sortBy (_._1) collect {
        case (code, name) ⇒ """<li><a lang="%s" href="%s"%s>%s</a></li>""".format(
          code,
          langUrl(Lang(code))(i18nDomain),
          (code == lang.language) ?? """ class="current"""",
          name)
      } mkString
    }).replace(uriPlaceholder, ctx.req.uri)
  }

  def commonDomain(implicit ctx: Context): String =
    I18nDomain(ctx.req.domain).commonDomain

  val uriPlaceholder = "[URI]"

  private def langUrl(lang: Lang)(i18nDomain: I18nDomain) =
    protocol + (i18nDomain withLang lang).domain + uriPlaceholder
}
