package lila
package i18n

import play.api.mvc.{ Action, RequestHeader, Handler }
import play.api.i18n.Lang

final class I18nPool(val langs: Set[Lang], val default: Lang) {

  private val cache = scala.collection.mutable.Map[String, Option[Lang]]()

  val names: Map[String, String] = langs map { l ⇒
    l.language -> LangList.name(l.language)
  } toMap

  def lang(req: RequestHeader) = domainLang(req) getOrElse default

  def preferred(req: RequestHeader) =
    (req.acceptLanguages find langs.contains) | default

  def domainLang(req: RequestHeader) = 
    cache.getOrElseUpdate(req.domain, {
      I18nDomain(req.domain).lang filter langs.contains 
    })
}
