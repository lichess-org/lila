package lila.i18n

import play.api.i18n.Lang
import play.api.mvc.{ Action, RequestHeader, Handler }

private[i18n] case class I18nPool(val langs: Set[Lang], val default: Lang) {

  def nonDefaultLangs = langs - default

  val names: Map[String, String] = (langs map langNames).toMap

  val contains: Set[String] = langs.map(_.language)

  private def langNames(lang: Lang): (String, String) =
    lang.language -> LangList.nameOrCode(lang.language)

  def lang(req: RequestHeader) = domainLang(req) getOrElse default

  def preferred(req: RequestHeader) =
    (req.acceptLanguages find langs.contains) getOrElse default

  def preferredNames(req: RequestHeader, nb: Int): Seq[(String, String)] =
    req.acceptLanguages filter langs.contains take nb map langNames

  private val domainLangCache = scala.collection.mutable.Map[String, Option[Lang]]()
  def domainLang(req: RequestHeader): Option[Lang] =
    domainLangCache.getOrElseUpdate(req.domain, {
      domainOf(req).lang filter langs.contains
    })

  private val domainCache = scala.collection.mutable.Map[String, I18nDomain]()
  def domainOf(req: RequestHeader): I18nDomain =
    domainCache.getOrElseUpdate(req.domain, { I18nDomain(req.domain) })
}
