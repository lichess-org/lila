package lila.i18n

import play.api.i18n.Lang
import play.api.mvc.Results.Redirect
import play.api.mvc.{ Action, RequestHeader, Handler }

final class I18nRequestHandler(pool: I18nPool, protocol: String) {

  def apply(req: RequestHeader): Option[Handler] =
    if (lila.common.HTTPRequest isSocket req) None
    else pool.domainLang(req).isDefined.fold(
      None,
      Action {
        Redirect(redirectUrl(req))
      } some
    )

  private def redirectUrl(req: RequestHeader) =
    protocol +
      I18nDomain(req.domain).withLang(pool preferred req).domain +
      req.uri
}
