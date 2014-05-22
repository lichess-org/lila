package lila.i18n

import play.api.i18n.Lang
import play.api.mvc.Results.Redirect
import play.api.mvc.{ Action, RequestHeader, Handler }

import lila.common.HTTPRequest

final class I18nRequestHandler(pool: I18nPool, protocol: String) {

  def apply(req: RequestHeader): Option[Handler] =
    (HTTPRequest.isRedirectable(req) && !pool.domainLang(req).isDefined) option Action {
      Redirect(redirectUrl(req))
    }

  private def redirectUrl(req: RequestHeader) =
    protocol +
      I18nDomain(req.domain).withLang(pool preferred req).domain +
      req.uri
}
