package lila
package templating

import controllers._
import i18n.{ LangList, I18nDomain }
import Global.env // OMG

import play.api.i18n.Lang
import play.api.templates.Html
import play.api.mvc.RequestHeader

trait I18nHelper {

  private val pool = env.i18nPool

  val trans = env.i18nKeys

  implicit def lang(implicit req: RequestHeader) = pool lang req

  def langName(lang: Lang) = LangList name lang.language

  def langUrl(lang: Lang)(implicit req: RequestHeader) = 
    (I18nDomain(req.domain) withLang lang).domain + req.uri

  def otherLangLinks(lang: Lang)(implicit req: RequestHeader) = Html {
    val url = langUrl(Lang("xx"))
    pool.names collect { 
      case (code, name) if code != lang.language =>
        """<li><a lang="%s" href="%s">%s</a></li>"""
          .format(code, url.replace("xx", code), name)
    } mkString
  } 
}
