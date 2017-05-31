package lila.i18n

import play.api.http.HeaderNames
import play.api.mvc._
import play.api.mvc.Results.MovedPermanently

import lila.common.HTTPRequest

final class SubdomainKiller(domain: String) {

  def apply(req: RequestHeader): Option[Handler] =
    if (appliesTo(req) && !allowMobileEn(req))
      Some(Action(MovedPermanently {
        val protocol = s"http${if (req.secure) "s" else ""}"
        s"$protocol://$domain${req.uri}"
      }))
    else None

  private def appliesTo(req: RequestHeader) =
    req.host.lift(2).has('.') &&
      req.host.drop(3) == domain &&
      HTTPRequest.isRedirectable(req) &&
      !excludePath(req.path)

  private def allowMobileEn(req: RequestHeader) =
    req.host.startsWith("en.") &&
      req.headers.get(HeaderNames.ACCEPT).exists(_ startsWith "application/vnd.lichess.v")

  private def excludePath(path: String) =
    path.contains("/embed/") || path.startsWith("/api/")
}
