package lila.i18n

import play.api.i18n.Lang
import scala.util.Try

case class I18nDomain(domain: String) {

  val parts = domain.split('.').toList

  def code: Option[String] = parts.headOption.filter(_.size == 2)

  val lang: Option[Lang] =
    code flatMap { c =>
      Try(Lang(c, "")).toOption
    }

  val valid: Boolean = code ?? { c => Try(Lang(c, "")).isSuccess }

  def hasLang = lang.isDefined

  val commonDomain = hasLang.fold(parts drop 1 mkString ".", domain)

  def withLang(lang: Lang): I18nDomain = withLang(lang.language)

  def withLang(lang: String): I18nDomain = I18nDomain(lang + "." + commonDomain)
}
