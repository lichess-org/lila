package lila.security

import play.api.mvc.RequestHeader

import lila.common.HTTPRequest

private[security] final class Wiretap(ips: Set[String]) {

  def apply(req: RequestHeader) {
    if (ips(req.remoteAddress)) loginfo("[wiretap] %s %s %s".format(
      req.remoteAddress,
      HTTPRequest.fullUrl(req),
      HTTPRequest.userAgent(req) | "?"
    ))
  }
}
