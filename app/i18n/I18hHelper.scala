package lila
package i18n

import core.CoreEnv
import controllers._
import http.Context

import play.api.i18n.Lang
import play.api.templates.Html
import play.api.mvc.{ RequestHeader, Call }

trait I18nHelper {

  protected def env: CoreEnv

  private val pool = env.i18n.pool

  val trans = env.i18n.keys

  implicit def lang(implicit ctx: Context) = pool lang ctx.req

  def langName(lang: Lang) = LangList name lang.language

  private lazy val otherLangLinksCache = 
    scala.collection.mutable.Map[String, String]()

  def otherLangLinks(lang: Lang)(implicit ctx: Context) = Html {
    otherLangLinksCache.getOrElseUpdate(lang.language, {
      pool.names.toList sortBy (_._1) collect {
        case (code, name) if code != lang.language ⇒
          """<li><a lang="%s" href="%s">%s</a></li>"""
            .format(code, langUrl(Lang(code))(ctx.req), name)
      } mkString
    })
  }

  def commonDomain(implicit ctx: Context): String = 
    I18nDomain(ctx.req.domain).commonDomain

  val protocol = "http://"

  private def langUrl(lang: Lang)(req: RequestHeader) =
    protocol + (I18nDomain(req.domain) withLang lang).domain + req.uri
}
