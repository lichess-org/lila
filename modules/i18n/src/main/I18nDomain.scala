package lila.i18n

import play.api.i18n.Lang

case class I18nDomain(domain: String) {

  lazy val parts = domain.split('.').toList

  lazy val lang: Option[Lang] =
    parts.headOption.filter(_.size == 2) map { Lang(_, "") }

  def hasLang = lang.isDefined

  lazy val commonDomain = hasLang.fold(parts drop 1 mkString ".", domain)

  def withLang(lang: Lang): I18nDomain = withLang(lang.language)

  def withLang(lang: String): I18nDomain = I18nDomain(s"$lang.$commonDomain")
}
