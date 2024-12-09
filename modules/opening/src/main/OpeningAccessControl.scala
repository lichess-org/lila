package lila.opening

import play.api.mvc.RequestHeader
import lila.memo.RateLimit.RateLimiter
import lila.common.HTTPRequest
import lila.core.security.Ip2ProxyApi
import lila.core.net.Crawler

final class OpeningAccessControl(proxyApi: Ip2ProxyApi, proxyLimiter: RateLimiter[UserId])(using
    req: RequestHeader,
    me: Option[Me]
)(using
    lila.core.config.RateLimit,
    Executor
):
  def canLoadExpensiveStats(wikiMarkup: Boolean, crawler: Crawler): Fu[Boolean] =
    if crawler.yes && !wikiMarkup
    then fuccess(false) // nothing for crawlers to index if we don't have our own text
    else
      proxyApi(HTTPRequest.ipAddress(req)).map: proxy =>
        if !proxy.is then true // legit IPs are always allowed
        else                   // only allow proxy IPs if they have a session
          me.exists: me =>
            // rate limit requests from proxy IPs per user
            proxyLimiter[Boolean](me.userId, default = false, cost = 1)(true)
