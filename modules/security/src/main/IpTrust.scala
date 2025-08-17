package lila.security

import play.api.mvc.RequestHeader

import lila.common.HTTPRequest
import lila.core.net.IpAddress
import lila.core.security.{ Ip2ProxyApi, IsProxy }

final class IpTrust(proxyApi: Ip2ProxyApi, geoApi: GeoIP, firewallApi: Firewall)(using
    Executor,
    lila.core.config.RateLimit
):
  import IpTrust.*

  private[security] def isSuspicious(ip: IpAddress): Fu[Boolean] =
    if firewallApi.blocksIp(ip) then fuTrue
    else if geoApi.isSuspicious(ip) then fuTrue
    else proxyApi.ofIp(ip).dmap(_.is)

  private[security] def isSuspicious(ipData: UserLogins.IPData): Fu[Boolean] =
    isSuspicious(ipData.ip.value)

  def ipData(ip: IpAddress): Fu[IpData] =
    proxyApi.ofIp(ip).dmap(IpData(_, geoApi.orUnknown(ip)))

  def reqData(req: RequestHeader): Fu[IpData] =
    proxyApi.ofReq(req).dmap(IpData(_, geoApi.orUnknown(HTTPRequest.ipAddress(req))))

  def isPubOrTor(req: RequestHeader): Fu[Boolean] = proxyApi
    .ofReq(req)
    .dmap:
      case IsProxy.public | IsProxy.tor => true
      case _ => false

  final class rateLimit(
      credits: Int,
      duration: FiniteDuration,
      key: String,
      strategy: IpTrust.type => RateLimitStrategy = _.defaultRateLimitStrategy
  ):
    import lila.memo.RateLimit as RL
    private val limiter = RL[IpAddress](credits, duration, key)

    def apply[A](default: => Fu[A], cost: RL.Cost = 1, msg: => String = "")(using
        req: RequestHeader
    )(
        op: => Fu[A]
    )(using Executor): Fu[A] =
      val ip = HTTPRequest.ipAddress(req)
      for
        proxy <- proxyApi.ofReq(req)
        ipCostFactor =
          if HTTPRequest.nginxWhitelist(req) then 1
          else strategy(IpTrust)(proxy)
        res <- limiter[Fu[A]](ip, default, (cost * ipCostFactor).toInt, s"$msg proxy:$proxy")(op)
      yield res

  def rateLimitCostFactor(
      req: RequestHeader,
      strategy: IpTrust.type => RateLimitStrategy = _.defaultRateLimitStrategy
  ): Fu[Float] =
    proxyApi.ofReq(req).dmap(strategy(IpTrust))

  def throttle(base: MaxPerSecond)(using req: RequestHeader)(using Executor): Fu[MaxPerSecond] =
    if HTTPRequest.nginxWhitelist(req) then fuccess(base)
    else
      proxyApi
        .ofReq(req)
        .map(defaultThrottleStrategy)
        .map: div =>
          base.map(mps => (mps / div).toInt)

object IpTrust:

  case class IpData(proxy: IsProxy, location: Location)

  type RateLimitStrategy = IsProxy => Float

  // https://blog.ip2location.com/knowledge-base/what-are-the-proxy-types-supported-in-ip2proxy/
  val defaultRateLimitStrategy: RateLimitStrategy =
    case IsProxy.vpn => 3
    case IsProxy.privacy => 3
    case IsProxy.tor => 4
    case IsProxy.server => 1.5
    case IsProxy.enterprise => 1.5
    case IsProxy.public => 5
    case IsProxy.web => 3
    case IsProxy.search => 0.5
    case IsProxy.residential => 3
    case _ => 1

  def proxyMultiplier(times: Float): RateLimitStrategy =
    case IsProxy.empty => 1
    case IsProxy.search => 0.5 // search factor is < 1 so don't multiply it
    case proxy => defaultRateLimitStrategy(proxy) * times

  def antiScraping(dch: Float, others: Float): RateLimitStrategy =
    case IsProxy.server => dch
    case IsProxy.search => 0.5 // search factor is < 1 so don't multiply it
    case proxy => defaultRateLimitStrategy(proxy) * others

  type ThrottleStrategy = IsProxy => Float

  val defaultThrottleStrategy: ThrottleStrategy =
    case IsProxy.server => 1
    case IsProxy.enterprise => 1
    case IsProxy.search => 1
    case p => defaultRateLimitStrategy(p)
