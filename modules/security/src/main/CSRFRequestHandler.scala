package lila.security

import play.api.mvc.Results.Forbidden
import play.api.mvc.{ Action, RequestHeader, Handler }

import lila.common.HTTPRequest._

final class CSRFRequestHandler(domain: String) {

  private def logger = lila.log("csrf")

  def apply(req: RequestHeader): Option[Handler] =
    if (isSafe(req)) None
    else origin(req).orElse(referer(req)) match {
      case None =>
        lila.mon.http.csrf.missingOrigin()
        logger.debug(print(req))
        None
      case Some(o) if isSubdomain(o) =>
        None
      case Some(o) =>
        lila.mon.http.csrf.forbidden()
        logger.info(print(req))
        // forbid(req).some
        None // only log for now
    }

  private val topDomain = s"://$domain"
  private val subDomain = s".$domain"

  // origin = "https://en.lichess.org"
  // domain = "lichess.org"
  private def isSubdomain(origin: String) =
    origin.endsWith(subDomain) || origin.endsWith(topDomain)

  private def forbid(req: RequestHeader): Handler =
    Action(Forbidden("Cross origin request forbidden"))
}
