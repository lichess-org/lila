package lila.i18n

import play.api.i18n.Lang
import scalalib.model.{ LangTag, Language }

import lila.common.HTTPRequest
import lila.core.i18n.{
  defaultLang,
  defaultLanguage,
  fixJavaLanguage,
  playAcceptLanguages as acceptLanguages,
  toLanguage
}
import lila.core.net.CountryCode

object LangPicker extends lila.core.i18n.LangPicker:

  def apply(req: Req, userLang: Option[LangTag] = None): Lang =
    userLang
      .orElse(LangTag.from(req.session.get("lang")))
      .flatMap(toLang)
      .flatMap(findCloser)
      .orElse(bestFromRequestHeaders(req))
      .getOrElse(defaultLang)

  def bestFromRequestHeaders(req: Req): Option[Lang] =
    acceptLanguages(req).collectFirstSome(findCloser)

  def allFromRequestHeaders(req: Req): List[Lang] =
    allFromRequestHeaders(acceptLanguages(req), HTTPRequest.country(using req))

  def allFromRequestHeaders(langs: Seq[Lang], ipCountry: Option[CountryCode]): List[Lang] = {
    langs.flatMap(findCloser) ++
      langs.flatMap(lang => ~byCountry.get(lang.country)) ++
      ipCountry.so(country => ~byCountry.get(country.value))
  }.distinct.toList

  def byStr(str: String): Option[Lang] =
    Lang.get(str).flatMap(findCloser)

  def byLangTagOrDefault(lt: Option[LangTag]): Lang =
    LangTag.raw(lt).flatMap(byStr) | defaultLang

  def sortFor(langs: List[Lang], req: Req): List[Lang] =
    val mine = allFromRequestHeaders(req).zipWithIndex.toMap
    langs.sortBy(mine.getOrElse(_, Int.MaxValue))

  def preferedLanguages(req: Req, prefLang: Lang): List[Language] = {
    toLanguage(prefLang) +: acceptLanguages(req).map(toLanguage)
  }.distinct.view.filter(LangList.popularLanguages.contains).toList

  def pickBestOf(
      candidates: Set[Language]
  )(req: Req, userLang: Option[String] = None): Option[Language] =
    userLang
      .flatMap(Lang.get)
      .map(toLanguage)
      .filter(candidates.contains)
      .orElse:
        acceptLanguages(req)
          .map(toLanguage)
          .collectFirst:
            case l if candidates.contains(l) => l
      .orElse(candidates.contains(defaultLanguage).option(defaultLanguage))

  object pageOverride:
    val qs = "en"
    def check(req: Req): Boolean = lila.common.HTTPRequest.queryStringBool(qs)(using req)
    def get(req: Req): Option[Lang] = check(req).option(defaultLang)

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

  def byHref(language: Language, req: Req): ByHref =
    val accepted = acceptLanguages(req)
    Lang.get(language.value).flatMap(findCloser) match
      case Some(lang) if fixJavaLanguage(lang) == language =>
        if accepted.isEmpty || accepted.exists(_.language == lang.language)
        then ByHref.Found(lang)
        else ByHref.Refused(lang)
      case Some(lang) => ByHref.Redir(fixJavaLanguage(lang))
      case None => ByHref.NotFound

  enum ByHref:
    case Found(lang: Lang)
    case Refused(lang: Lang)
    case Redir(language: Language)
    case NotFound
