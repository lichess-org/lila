package lila
package i18n

import play.api.Application
import play.api.mvc._
import play.api.mvc.Results._
import play.api.i18n._

final class I18nRequestHandler(langs: Set[Lang], default: Lang) {

  val protocol = "http://"

  private case class I18nDomain(domain: String) {

    lazy val parts = domain.split('.').toList

    def lang: Option[Lang] = 
      parts.headOption filter (_.size == 2) map { Lang(_, "") }

    def hasLang = lang.isDefined

    def commonDomain = hasLang.fold(parts drop 1 mkString ".", domain)

    def withLang(lang: Lang) = I18nDomain(lang.language + "." + commonDomain)
  }

  def apply(request: RequestHeader): Option[Handler] = {
    val domain = I18nDomain(request.domain)
    domain.lang.filter(langs.contains).isDefined.fold(
      None,
      Action {
        Redirect(
          protocol + domain.withLang(preferred(request)).domain
        )
      } some
    )
  }

  def preferred(request: RequestHeader) = 
    (request.acceptLanguages find langs.contains) | default
}
