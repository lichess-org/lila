package lila.i18n

import play.api.mvc.Results.Redirect
import play.api.mvc.{ Action, RequestHeader, Handler, Result }

import lila.common.HTTPRequest

final class I18nRequestHandler(
    pool: I18nPool,
    protocol: String,
    cdnDomain: String) {

  def apply(req: RequestHeader): Option[Handler] =
    if (HTTPRequest.isRedirectable(req) &&
      req.host != cdnDomain &&
      pool.domainLang(req).isEmpty &&
      !excludePath(req.path)) Some(Action(Redirect(redirectUrl(req))))
    else None

  def forUser(req: RequestHeader, userOption: Option[lila.user.User]): Option[Result] = for {
    userLang <- userOption.flatMap(_.lang)
    if HTTPRequest.isRedirectable(req)
    reqLang <- pool domainLang req
    if userLang != reqLang.language
  } yield Redirect(redirectUrlLang(req, userLang))

  private def excludePath(path: String) = path.contains("/embed/")

  private def redirectUrl(req: RequestHeader) =
    redirectUrlLang(req, pool.preferred(req).language)

  private def redirectUrlLang(req: RequestHeader, lang: String) =
    s"$protocol${I18nDomain(req.domain).withLang(lang).domain}${req.uri}"
}
