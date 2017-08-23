package lila.security

import play.api.mvc.RequestHeader

import lila.common.HTTPRequest._

final class CSRFRequestHandler(domain: String, enabled: Boolean) {

  private def logger = lila.log("csrf")

  def check(req: RequestHeader): Boolean = {
    if (isXhr(req) || (isSafe(req) && !isSocket(req))) true
    else origin(req).orElse(referer(req) flatMap refererToOrigin) match {
      case None =>
        lila.mon.http.csrf.missingOrigin()
        logger.debug(print(req))
        true
      case Some("file://") =>
        true
      case Some(o) if isSubdomain(o) =>
        true
      case Some(_) =>
        if (isSocket(req)) {
          lila.mon.http.csrf.websocket()
          logger.info(s"WS ${print(req)}")
        } else {
          lila.mon.http.csrf.forbidden()
          logger.info(print(req))
        }
        !enabled // ignore if disabled
    }
  }

  private val topDomain = s"://$domain"
  private val subDomain = s".$domain"

  // origin = "https://lichess.org"
  // domain = "lichess.org"
  private def isSubdomain(origin: String) =
    origin.endsWith(subDomain) || origin.endsWith(topDomain)

  // input  = "https://lichess.org/some/path?a=b&c=d"
  // output = "https://lichess.org"
  private val RefererToOriginRegex = """^([^:]+://[^/]+).*""".r // a.k.a. pokemon face regex
  private def refererToOrigin(r: String): Option[String] = r match {
    case RefererToOriginRegex(origin) => origin.some
    case _ => none
  }
}
