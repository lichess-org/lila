package lila.security

import lila.common.IpAddress

final class IpTrust(proxyApi: Ip2Proxy, geoApi: GeoIP, torApi: Tor, firewallApi: Firewall):

  private[security] def isSuspicious(ip: IpAddress): Fu[Boolean] =
    if firewallApi blocksIp ip then fuTrue
    else if torApi isExitNode ip then fuTrue
    else proxyApi(ip).dmap(_.is)

  private[security] def isSuspicious(ipData: UserLogins.IPData): Fu[Boolean] =
    isSuspicious(ipData.ip.value)

  private def data(ip: IpAddress): Fu[IpTrust.IpData] =
    val location = geoApi orUnknown ip
    val tor      = torApi isExitNode ip
    proxyApi(ip).dmap(IpTrust.IpData(_, location, tor))

  final class rateLimit(credits: Int, duration: FiniteDuration, key: String, factor: Int = 3):
    import lila.memo.{ RateLimit as RL }
    private val limiter = RL[IpAddress](credits, duration, key)
    def apply[A](ip: IpAddress, default: => Fu[A], cost: RL.Cost = 1, msg: => String = "")(op: => Fu[A])(using
        Executor
    ): Fu[A] =
      isSuspicious(ip).flatMap: susp =>
        val realCost = cost * (if susp then factor else 1)
        limiter[Fu[A]](ip, default, realCost, msg)(op)

object IpTrust:

  case class IpData(proxy: IsProxy, location: Location, isTor: Boolean)
