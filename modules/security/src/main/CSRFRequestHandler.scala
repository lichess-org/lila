package lila.security

import play.api.mvc.RequestHeader

import lila.common.HTTPRequest._

final class CSRFRequestHandler(domain: String) {

  private def logger = lila.log("csrf")

  def check(req: RequestHeader): Boolean = {
    if (isXhr(req)) true // cross origin xhr not allowed by browsers
    else if (isSafe(req)) true
    else if (appOrigin(req).isDefined) true
    else origin(req) match {
      case None =>
        lila.mon.http.csrf.missingOrigin()
        logger.debug(print(req))
        true
      case Some(o) if isSubdomain(o) =>
        true
      case Some(_) =>
        lila.mon.http.csrf.forbidden()
        logger.info(print(req))
        false
    }
  }

  private val topDomain = s"://$domain"
  private val subDomain = s".$domain"

  // origin = "https://lichess.org"
  // domain = "lichess.org"
  private def isSubdomain(origin: String) =
    origin.endsWith(subDomain) || origin.endsWith(topDomain)
}
