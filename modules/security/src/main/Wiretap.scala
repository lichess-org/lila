package lila.security

import play.api.mvc.RequestHeader

final class Wiretap(ips: Set[String]) {

  def apply(req: RequestHeader) {
    if (ips(req.remoteAddress)) println("[wiretap] %s %s %s".format(
      req.remoteAddress,
      "http://" + req.host + req.uri,
      req.headers.get("User-Agent") | "?"
    ))
  }
}
