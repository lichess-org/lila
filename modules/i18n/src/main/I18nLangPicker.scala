package lila.i18n

import play.api.i18n.Lang
import play.api.mvc.RequestHeader

import lila.user.User

final class I18nLangPicker(pool: I18nPool) {

  def apply(req: RequestHeader, user: Option[User]): Lang = {
    user.flatMap(_.lang).orElse(req.session get "lang").map(Lang.get) match {
      case Some(lang) => findCloser(lang) orElse {
        pool.domainLang(req) match {
      }
      // // user has a lang that doesn't match the request, redirect to user lang
      // case Some(userLang) if !pool.domainLang(req).exists(_.language == userLang) =>
      //   Redirect(redirectUrlLang(req, userLang)).some
      // // no user lang
      // case None => pool.domainLang(req) match {
      //   // header accepts the req lang, just proceed
      //   case Some(reqLang) if req.acceptLanguages.has(reqLang) => none
      //   // header refuses the req lang, redirect if a better lang can be found
      //   case Some(reqLang) =>
      //     val preferred = pool preferred req
      //     (preferred != reqLang) option Redirect(redirectUrlLang(req, preferred.language))
      //   // no req lang, redirect based on header
      //   case None => Redirect(redirectUrlLang(req, pool.preferred(req).language)).some
      // }
      // case _ => none
    }
  }

  private val defaultByLanguage: Map[String, Lang] =
    pool.langs.foldLeft(Map.empty[String, Lang]) {
      case (acc, lang) => acc + (lang.language -> lang)
    }

  private def findCloser(to: Lang): Option[Lang] =
    if (pool.langs contains to) Some(to)
    else lichessCodes.get(to.language) orElse
      defaultByLanguage.get(to.language)
}

  object I18nLangPicker {
  }
