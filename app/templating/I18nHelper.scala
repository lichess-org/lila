package lila
package templating

import controllers._
import http.Context
import i18n.{ LangList, I18nDomain }
import Global.env // OMG

import play.api.i18n.Lang
import play.api.templates.Html
import play.api.mvc.RequestHeader

trait I18nHelper {

  private val pool = env.i18nPool

  val trans = env.i18nKeys

  implicit def lang(implicit ctx: Context) = pool lang ctx.req

  def langName(lang: Lang) = LangList name lang.language

  private lazy val otherLangLinksCache = 
    scala.collection.mutable.Map[String, String]()

  def otherLangLinks(lang: Lang)(implicit ctx: Context) = Html {
    otherLangLinksCache.getOrElseUpdate(lang.language, {
      pool.names collect {
        case (code, name) if code != lang.language ⇒
          """<li><a lang="%s" href="%s">%s</a></li>"""
            .format(code, langUrl(Lang(code))(ctx.req), name)
      } mkString
    })
  }

  private def langUrl(lang: Lang)(req: RequestHeader) =
    (I18nDomain(req.domain) withLang lang).domain + req.uri
}
