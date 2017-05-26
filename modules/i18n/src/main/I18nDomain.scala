package lila.i18n

import play.api.i18n.Lang

case class I18nDomain(domain: String) {

  lazy val parts = domain.split('.').toList

  // may not be available
  lazy val langCode: Option[String] = {
    val code = parts.headOption.filter(_.size == 2)
    lichessCodes.getOrElse(code, code)
  }

  def hasLangCode = lang.isDefined

  lazy val commonDomain = hasLang.fold(parts drop 1 mkString ".", domain)

  def withLang(lang: Lang): I18nDomain = withLanguage(lang.language)

  def withLanguage(lang: String): I18nDomain = I18nDomain {
    s"$lang.$commonDomain"
  }
}
