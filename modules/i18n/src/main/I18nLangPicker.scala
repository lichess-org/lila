package lila.i18n

import play.api.i18n.Lang
import play.api.mvc.RequestHeader

import lila.user.User

object I18nLangPicker {

  def apply(req: RequestHeader, user: Option[User]): Lang =
    user
      .flatMap(_.lang)
      .orElse(req.session get "lang")
      .flatMap(Lang.get)
      .flatMap(findCloser)
      .orElse(bestFromRequestHeaders(req))
      .getOrElse(defaultLang)

  def bestFromRequestHeaders(req: RequestHeader): Option[Lang] =
    req.acceptLanguages.foldLeft(none[Lang]) {
      case (None, lang) => findCloser(lang)
      case (found, _) => found
    }

  def allFromRequestHeaders(req: RequestHeader): List[Lang] =
    req.acceptLanguages.flatMap(findCloser).distinct.toList

  def byStr(str: String): Option[Lang] =
    Lang get str flatMap findCloser

  private val defaultByLanguage: Map[String, Lang] =
    I18nDb.langs.foldLeft(Map.empty[String, Lang]) {
      case (acc, lang) => acc + (lang.language -> lang)
    }

  def findCloser(to: Lang): Option[Lang] =
    if (I18nDb.langs contains to) Some(to)
    else defaultByLanguage.get(to.language) orElse
      lichessCodes.get(to.language)
}
