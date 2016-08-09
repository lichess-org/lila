package lila.i18n

import play.api.i18n.Lang
import play.api.mvc.{ Action, RequestHeader, Handler }

private[i18n] case class I18nPool(val langs: Set[Lang], val default: Lang) {

  private val cache = scala.collection.mutable.Map[String, Option[Lang]]()

  def nonDefaultLangs = langs - default

  val names: Map[String, String] = (langs map langNames).toMap

  val contains: Set[String] = langs.map(_.language)

  private def langNames(lang: Lang): (String, String) =
    lang.language -> LangList.nameOrCode(lang.language)

  def lang(req: RequestHeader) =
    withReqCountry(domainLang(req) getOrElse default, req)

  private val nonUsEnglish = Lang("en", "gb")

  private def withReqCountry(lang: Lang, req: RequestHeader) = req.acceptLanguages.find { l =>
    l.language == lang.language
  }.getOrElse(lang) match {
    case Lang("en", "") => nonUsEnglish
    case l              => l
  }

  def preferred(req: RequestHeader) =
    (req.acceptLanguages find langs.contains) getOrElse default

  def domainLang(req: RequestHeader) =
    cache.getOrElseUpdate(req.domain, {
      I18nDomain(req.domain).lang filter langs.contains
    })
}
