package lila
package i18n

import play.api.i18n.Lang

final class I18nDomain private (val domain: String) {

  lazy val parts = domain.split('.').toList

  lazy val lang: Option[Lang] =
    parts.headOption filter (_.size == 2) map { Lang(_, "") }

  def hasLang = lang.isDefined

  lazy val commonDomain = hasLang.fold(parts drop 1 mkString ".", domain)

  def withLang(lang: Lang) = I18nDomain(lang.language + "." + commonDomain)
}

object I18nDomain {

  def apply(domain: String): I18nDomain = new I18nDomain(domain)
}
