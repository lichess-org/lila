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
    (fixedReqAcceptLanguages(req) find langs.contains) | default

  // the original implementation prints a stacktrace
  // when the header is malformed.
  def fixedReqAcceptLanguages(req: RequestHeader) = try {
    req.headers.get(play.api.http.HeaderNames.ACCEPT_LANGUAGE).map { acceptLanguage ⇒
      acceptLanguage.split(",").map(l ⇒ play.api.i18n.Lang(l.split(";").head)).toSeq
    }.getOrElse(Nil)
  }
  catch {
    case e ⇒ Nil
  }

  def domainLang(req: RequestHeader) =
    cache.getOrElseUpdate(req.domain, {
      I18nDomain(req.domain).lang filter langs.contains
    })
}
