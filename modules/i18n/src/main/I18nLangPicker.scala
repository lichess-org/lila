package lila.i18n

import play.api.mvc.RequestHeader
import play.api.i18n.Lang

object I18nLangPicker:

  def apply(req: RequestHeader, userLang: Option[String] = None): Lang =
    userLang
      .orElse(req.session get "lang")
      .flatMap(Lang.get)
      .flatMap(findCloser)
      .orElse(bestFromRequestHeaders(req))
      .getOrElse(defaultLang)

  def bestFromRequestHeaders(req: RequestHeader): Option[Lang] =
    req.acceptLanguages.foldLeft(none[Lang]):
      case (None, lang) => findCloser(lang)
      case (found, _)   => found

  def allFromRequestHeaders(req: RequestHeader): List[Lang] = {
    req.acceptLanguages.flatMap(findCloser) ++
      req.acceptLanguages.flatMap(lang => ~byCountry.get(lang.country))
  }.distinct.toList

  def byStr(str: String): Option[Lang] =
    Lang get str flatMap findCloser

  def byStrOrDefault(str: Option[String]): Lang =
    str.flatMap(byStr) | defaultLang

  def sortFor(langs: List[Lang], req: RequestHeader): List[Lang] =
    val mine = allFromRequestHeaders(req).zipWithIndex.toMap
    langs.sortBy { mine.getOrElse(_, Int.MaxValue) }

  private val defaultByLanguage: Map[String, Lang] =
    LangList.all.keys
      .foldLeft(Map.empty[String, Lang]): (acc, lang) =>
        acc + (lang.language -> lang)
      .++(LangList.defaultRegions)

  private val byCountry: Map[String, List[Lang]] =
    LangList.all.keys.toList.groupBy(_.country)

  def findCloser(to: Lang): Option[Lang] =
    if LangList.all.keySet contains to then Some(to)
    else
      defaultByLanguage.get(to.language) orElse
        lichessCodes.get(to.language)

  def byHref(code: String, req: RequestHeader): ByHref =
    Lang get code flatMap findCloser match
      case Some(lang) if fixJavaLanguageCode(lang) == code =>
        if req.acceptLanguages.isEmpty || req.acceptLanguages.exists(_.language == lang.language)
        then ByHref.Found(lang)
        else ByHref.Refused(lang)
      case Some(lang) => ByHref.Redir(fixJavaLanguageCode(lang))
      case None       => ByHref.NotFound

  enum ByHref:
    case Found(lang: Lang)
    case Refused(lang: Lang)
    case Redir(code: String)
    case NotFound
