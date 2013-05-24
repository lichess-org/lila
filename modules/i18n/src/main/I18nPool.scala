package lila.i18n

import play.api.i18n.Lang
import play.api.mvc.{ Action, RequestHeader, Handler }

private[i18n] case class I18nPool(val langs: Set[Lang], val default: Lang) {

  private val cache = scala.collection.mutable.Map[String, Option[Lang]]()

  def nonDefaultLangs = langs - default

  val names: Map[String, String] = langs map { l ⇒
    l.language -> LangList.nameOrCode(l.language)
  } toMap

  def lang(req: RequestHeader) = domainLang(req) | default

  def preferred(req: RequestHeader) =
    (req.acceptLanguages find langs.contains) | default

  def domainLang(req: RequestHeader) =
    cache.getOrElseUpdate(req.domain, {
      I18nDomain(req.domain).lang filter langs.contains
    })
}
