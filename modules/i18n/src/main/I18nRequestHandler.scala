package lila.i18n

import play.api.mvc.Results.Redirect
import play.api.mvc.{ Action, RequestHeader, Handler, Result }

import lila.common.HTTPRequest

final class I18nRequestHandler(
    pool: I18nPool,
    protocol: String,
    cdnDomain: String
) {

  def apply(req: RequestHeader, userOption: Option[lila.user.User]): Option[Result] =
    if (appliesTo(req))
      userOption.flatMap(_.lang) match {
        // user has a lang that doesn't match the request, redirect to user lang
        case Some(userLang) if !pool.domainLang(req).exists(_.language == userLang) =>
          Redirect(redirectUrlLang(req, userLang)).some
        // no user lang
        case None => pool.domainLang(req) match {
          // header accepts the req lang, just proceed
          case Some(reqLang) if req.acceptLanguages.has(reqLang) => none
          // header refuses the req lang, redirect if a better lang can be found
          case Some(reqLang) =>
            val preferred = pool preferred req
            (preferred != reqLang) option Redirect(redirectUrlLang(req, preferred.language))
          // no req lang, redirect based on header
          case None => Redirect(redirectUrlLang(req, pool.preferred(req).language)).some
        }
        case _ => none
      }
    else none

  private def appliesTo(req: RequestHeader) =
    HTTPRequest.isRedirectable(req) && req.host != cdnDomain && !excludePath(req.path)

  private def excludePath(path: String) = path.contains("/embed/")

  private def redirectUrlLang(req: RequestHeader, lang: String) =
    s"$protocol${I18nDomain(req.domain).withLang(lang).domain}${req.uri}"
}
