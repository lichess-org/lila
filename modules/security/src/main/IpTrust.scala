package lila.security

import lila.common.IpAddress

final class IpTrust(proxyApi: Ip2Proxy, geoApi: GeoIP, firewallApi: Firewall):

  import IpTrust.*

  private[security] def isSuspicious(ip: IpAddress): Fu[Boolean] =
    if firewallApi blocksIp ip then fuTrue
    else proxyApi(ip).dmap(_.is)

  private[security] def isSuspicious(ipData: UserLogins.IPData): Fu[Boolean] =
    isSuspicious(ipData.ip.value)

  def data(ip: IpAddress): Fu[IpData] =
    proxyApi(ip).dmap(IpData(_, geoApi orUnknown ip))

  def isPubOrTor(ip: IpAddress): Fu[Boolean] = proxyApi(ip).dmap:
    case IsProxy.public | IsProxy.tor => true
    case _                            => false

  final class rateLimit(
      credits: Int,
      duration: FiniteDuration,
      key: String,
      strategy: IpTrust.type => RateLimitStrategy = _.defaultRateLimitStrategy
  ):
    import lila.memo.{ RateLimit as RL }
    private val limiter = RL[IpAddress](credits, duration, key)
    def apply[A](ip: IpAddress, default: => Fu[A], cost: RL.Cost = 1, msg: => String = "")(op: => Fu[A])(using
        Executor
    ): Fu[A] = for
      proxy <- proxyApi(ip)
      ipCostFactor = strategy(IpTrust)(proxy)
      res <- limiter[Fu[A]](ip, default, (cost * ipCostFactor).toInt, s"$msg proxy:$proxy")(op)
    yield res

  def rateLimitCostFactor(
      ip: IpAddress,
      strategy: IpTrust.type => RateLimitStrategy = _.defaultRateLimitStrategy
  ): Fu[Float] =
    proxyApi(ip).dmap(strategy(IpTrust))

object IpTrust:

  case class IpData(proxy: IsProxy, location: Location)

  type RateLimitStrategy = IsProxy => Float

  // https://blog.ip2location.com/knowledge-base/what-are-the-proxy-types-supported-in-ip2proxy/
  val defaultRateLimitStrategy: RateLimitStrategy =
    case IsProxy.vpn         => 2.5
    case IsProxy.tor         => 3.5
    case IsProxy.server      => 1.5
    case IsProxy.public      => 4.5
    case IsProxy.web         => 3
    case IsProxy.search      => 0.5
    case IsProxy.residential => 3
    case _                   => 1

  def proxyMultiplier(times: Float): RateLimitStrategy =
    case IsProxy.empty => 1
    case proxy         => defaultRateLimitStrategy(proxy) * times
