package lila.app
package i18n

import core.CoreEnv
import controllers._
import http.Context

import play.api.i18n.Lang
import play.api.templates.Html
import play.api.mvc.{ RequestHeader, Call }
import scala.util.Random.shuffle

trait I18nHelper {

  protected def env: CoreEnv

  private def pool = env.i18n.pool
  private def transInfos = env.i18n.transInfos
  private def hideCallsCookieName = env.i18n.hideCallsCookieName

  val trans = env.i18n.keys

  implicit def lang(implicit ctx: Context) = pool lang ctx.req

  def langName(lang: Lang) = LangList name lang.language

  def translationCall(implicit ctx: Context) =
    if (ctx.req.cookies.get(hideCallsCookieName).isDefined) None
    else shuffle(
      (pool.fixedReqAcceptLanguages(ctx.req) map transInfos.get).flatten filter (_.nonComplete)
    ).headOption

  def transValidationPattern(trans: String) =
    (trans contains "%s") option ".*%s.*"

  private lazy val otherLangLinksCache =
    scala.collection.mutable.Map[String, String]()

  def otherLangLinks(lang: Lang)(implicit ctx: Context) = Html {
    otherLangLinksCache.getOrElseUpdate(lang.language, {
      pool.names.toList sortBy (_._1) collect {
        case (code, name) if code != lang.language ⇒
          """<li><a lang="%s" href="%s">%s</a></li>"""
            .format(code, langUrl(Lang(code))(ctx.req), name)
      } mkString
    }).replace(uriPlaceholder, ctx.req.uri)
  }

  def commonDomain(implicit ctx: Context): String =
    I18nDomain(ctx.req.domain).commonDomain

  val protocol = "http://"
  val uriPlaceholder = "[URI]"

  private def langUrl(lang: Lang)(req: RequestHeader) =
    protocol + (I18nDomain(req.domain) withLang lang).domain + uriPlaceholder
}
