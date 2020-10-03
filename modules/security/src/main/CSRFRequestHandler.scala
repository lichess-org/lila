package lila.security

import play.api.mvc.RequestHeader

import lila.common.HTTPRequest._
import lila.common.config.NetConfig

final class CSRFRequestHandler(net: NetConfig) {

  def check(req: RequestHeader): Boolean = {
    if (isXhr(req)) true // cross origin xhr not allowed by browsers
    else if (isSafe(req)) true
    else if (appOrigin(req).isDefined) true
    else
      origin(req) match {
        case None =>
          monitor("missingOrigin", req)
          true
        case Some(o) if isSubdomain(o) =>
          true
        case Some(_) =>
          monitor("forbidden", req)
          false
      }
  }

  private def monitor(tpe: String, req: RequestHeader) =
    lila.mon.http.csrfError(tpe, actionName(req), clientName(req)).increment()

  private val topDomain = s"://${net.domain}"
  private val subDomain = s".${net.domain}"

  // origin = "https://lichess.org"
  // domain = "lichess.org"
  private def isSubdomain(origin: String) =
    origin.endsWith(subDomain) || origin.endsWith(topDomain)
}
