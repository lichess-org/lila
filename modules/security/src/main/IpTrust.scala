package lila.security

import lila.common.IpAddress

final class IpTrust(proxyApi: Ip2Proxy, geoApi: GeoIP, firewallApi: Firewall):

  private[security] def isSuspicious(ip: IpAddress): Fu[Boolean] =
    if firewallApi blocksIp ip then fuTrue
    else proxyApi(ip).dmap(_.is)

  private[security] def isSuspicious(ipData: UserLogins.IPData): Fu[Boolean] =
    isSuspicious(ipData.ip.value)

  def data(ip: IpAddress): Fu[IpTrust.IpData] =
    proxyApi(ip).dmap(IpTrust.IpData(_, geoApi orUnknown ip))

  def isPubOrTor(ip: IpAddress): Fu[Boolean] = proxyApi(ip).dmap:
    case IsProxy.pub | IsProxy.tor => true
    case _                         => false

  final class rateLimit(credits: Int, duration: FiniteDuration, key: String, factor: Int = 2):
    import lila.memo.{ RateLimit as RL }
    private val limiter = RL[IpAddress](credits, duration, key)
    def apply[A](ip: IpAddress, default: => Fu[A], cost: RL.Cost = 1, msg: => String = "")(op: => Fu[A])(using
        Executor
    ): Fu[A] =
      rateLimitCostFactor(ip, factor).flatMap: ipCostFactor =>
        limiter[Fu[A]](ip, default, cost * ipCostFactor, msg)(op)

  def rateLimitCostFactor(ip: IpAddress, factor: Int): Fu[Int] = proxyApi(ip).dmap:
    case IsProxy.empty => 1
    case IsProxy.pub   => factor * 3
    case _             => factor

object IpTrust:

  case class IpData(proxy: IsProxy, location: Location)
