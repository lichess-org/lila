package lila
package i18n

import play.api.mvc.{ Action, RequestHeader, Handler }
import play.api.i18n.Lang

final class I18nPool(val langs: Set[Lang], val default: Lang) {

  val names: Map[String, String] = langs map { l ⇒
    l.language -> LangList.name(l.language)
  } toMap

  def lang(implicit req: RequestHeader) = domainLang(req) | default

  def preferred(implicit req: RequestHeader) =
    (req.acceptLanguages find langs.contains) | default

  def domainLang(implicit req: RequestHeader) = 
    I18nDomain(req.domain).lang filter langs.contains 
}
