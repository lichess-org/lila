package lila.i18n

import play.api.mvc.RequestHeader
import play.api.i18n.Lang

object I18nLangPicker {

  def apply(req: RequestHeader, userLang: Option[String] = None): Lang =
    userLang
      .orElse(req.session get "lang")
      .flatMap(Lang.get)
      .flatMap(findCloser)
      .orElse(bestFromRequestHeaders(req))
      .getOrElse(defaultLang)

  def bestFromRequestHeaders(req: RequestHeader): Option[Lang] =
    req.acceptLanguages.foldLeft(none[Lang]) {
      case (None, lang) => findCloser(lang)
      case (found, _)   => found
    }

  def allFromRequestHeaders(req: RequestHeader): List[Lang] =
    req.acceptLanguages.flatMap(findCloser).distinct.toList

  def byStr(str: String): Option[Lang] =
    Lang get str flatMap findCloser

  def byStrOrDefault(str: Option[String]): Lang =
    str.flatMap(byStr) | defaultLang

  def sortFor(langs: List[Lang], req: RequestHeader): List[Lang] = {
    val mine = allFromRequestHeaders(req).zipWithIndex.toMap
    langs.sortBy { mine.getOrElse(_, Int.MaxValue) }
  }

  private val defaultByLanguage: Map[String, Lang] =
    Registry.langs.foldLeft(Map.empty[String, Lang]) { case (acc, lang) =>
      acc + (lang.language -> lang)
    }

  def findCloser(to: Lang): Option[Lang] =
    if (Registry.langs contains to) Some(to)
    else
      defaultByLanguage.get(to.language) orElse
        lichessCodes.get(to.language)
}
