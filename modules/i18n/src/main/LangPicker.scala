package lila.i18n

import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import scalalib.model.{ Language, LangTag }

import lila.core.i18n.{ toLanguage, defaultLang, defaultLanguage, fixJavaLanguage }

object LangPicker extends lila.core.i18n.LangPicker:

  def apply(req: RequestHeader, userLang: Option[LangTag] = None): Lang =
    userLang
      .orElse(LangTag.from(req.session.get("lang")))
      .flatMap(toLang)
      .flatMap(findCloser)
      .orElse(bestFromRequestHeaders(req))
      .getOrElse(defaultLang)

  def bestFromRequestHeaders(req: RequestHeader): Option[Lang] =
    req.acceptLanguages.collectFirstSome(findCloser)

  def allFromRequestHeaders(req: RequestHeader): List[Lang] = {
    req.acceptLanguages.flatMap(findCloser) ++
      req.acceptLanguages.flatMap(lang => ~byCountry.get(lang.country))
  }.distinct.toList

  def byStr(str: String): Option[Lang] =
    Lang.get(str).flatMap(findCloser)

  def byStrOrDefault(str: Option[String]): Lang =
    str.flatMap(byStr) | defaultLang

  def sortFor(langs: List[Lang], req: RequestHeader): List[Lang] =
    val mine = allFromRequestHeaders(req).zipWithIndex.toMap
    langs.sortBy { mine.getOrElse(_, Int.MaxValue) }

  def preferedLanguages(req: RequestHeader, prefLang: Lang): List[Language] = {
    toLanguage(prefLang) +: req.acceptLanguages.map(toLanguage)
  }.distinct.view.filter(LangList.popularLanguages.contains).toList

  def pickBestOf(
      candidates: Set[Language]
  )(req: RequestHeader, userLang: Option[String] = None): Option[Language] =
    userLang
      .flatMap(Lang.get)
      .map(toLanguage)
      .filter(candidates.contains)
      .orElse:
        req.acceptLanguages
          .map(toLanguage)
          .collectFirst:
            case l if candidates.contains(l) => l
      .orElse(candidates.contains(defaultLanguage).option(defaultLanguage))

  private val defaultByLanguage: Map[String, Lang] =
    LangList.all.keys
      .foldLeft(Map.empty[String, Lang]): (acc, lang) =>
        acc + (lang.language -> lang)
      .++(LangList.defaultRegions)

  private val byCountry: Map[String, List[Lang]] =
    LangList.all.keys.toList.groupBy(_.country)

  def findCloser(to: Lang): Option[Lang] =
    if LangList.all.keySet contains to then Some(to)
    else defaultByLanguage.get(to.language).orElse(lichessCodes.get(to.language))

  def byHref(language: Language, req: RequestHeader): ByHref =
    Lang.get(language.value).flatMap(findCloser) match
      case Some(lang) if fixJavaLanguage(lang) == language =>
        if req.acceptLanguages.isEmpty || req.acceptLanguages.exists(_.language == lang.language)
        then ByHref.Found(lang)
        else ByHref.Refused(lang)
      case Some(lang) => ByHref.Redir(fixJavaLanguage(lang))
      case None => ByHref.NotFound

  enum ByHref:
    case Found(lang: Lang)
    case Refused(lang: Lang)
    case Redir(language: Language)
    case NotFound
