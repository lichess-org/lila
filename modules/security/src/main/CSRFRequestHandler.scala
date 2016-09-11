package lila.security

import play.api.mvc.Results.Forbidden
import play.api.mvc.{ Action, RequestHeader, Result }

import lila.common.HTTPRequest._

final class CSRFRequestHandler(domain: String) {

  private def logger = lila.log("csrf")

  def check(req: RequestHeader): Boolean = {
    lazy val orig = origin(req).orElse(referer(req) flatMap refererToOrigin)

    if (isXhr(req)) true
    else if (isSocket(req)) {
      if (orig.exists(o => o == "file://" || isSubdomain(o))) true
      else {
        lila.mon.http.csrf.websocket()
        logger.info(s"WS ${print(req)}")
        true // TODO: false
      }
    }
    else if (isSafe(req)) true
    else orig match {
      case None =>
        lila.mon.http.csrf.missingOrigin()
        logger.debug(print(req))
        true // TODO: false
      case Some(o) if isSubdomain(o) =>
        true
      case Some(o) =>
        lila.mon.http.csrf.forbidden()
        logger.info(print(req))
        true // TODO: false
    }
  }

  def apply(req: RequestHeader): Option[Result] = {
    if (check(req)) None
    else Forbidden("Cross origin request forbidden").some
  }

  private val topDomain = s"://$domain"
  private val subDomain = s".$domain"

  // origin = "https://en.lichess.org"
  // domain = "lichess.org"
  private def isSubdomain(origin: String) =
    origin.endsWith(subDomain) || origin.endsWith(topDomain)

  // input  = "https://en.lichess.org/some/path?a=b&c=d"
  // output = "https://en.lichess.org"
  private val RefererToOriginRegex = """^([^:]+://[^/]+).*""".r // a.k.a. pokemon face regex
  private def refererToOrigin(r: String): Option[String] = r match {
    case RefererToOriginRegex(origin) => origin.some
    case _                            => none
  }
}
