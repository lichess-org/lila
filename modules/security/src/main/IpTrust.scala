package lila.security

import lila.common.IpAddress

final class IpTrust(proxyApi: Ip2Proxy, geoApi: GeoIP, torApi: Tor, firewallApi: Firewall):

  def isSuspicious(ip: IpAddress): Fu[Boolean] =
    if firewallApi blocksIp ip then fuTrue
    else if torApi isExitNode ip then fuTrue
    else
      val location = geoApi orUnknown ip
      if location == Location.unknown || location == Location.tor then fuTrue
      else if isUndetectedProxy(location) then fuTrue
      else proxyApi(ip).dmap(_.is)

  def isSuspicious(ipData: UserLogins.IPData): Fu[Boolean] =
    isSuspicious(ipData.ip.value)

  def data(ip: IpAddress): Fu[IpTrust.IpData] =
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

  /* lichess blacklist of proxies that ip2proxy doesn't know about */
  private def isUndetectedProxy(location: Location): Boolean =
    location.shortCountry == "Iran" ||
      location.shortCountry == "United Arab Emirates" || (location match
        case Location("Poland", _, Some("Subcarpathian Voivodeship"), Some("Stalowa Wola")) => true
        case Location("Poland", _, Some("Lesser Poland Voivodeship"), Some("Krakow"))       => true
        case Location("Russia", _, Some(region), Some("Ufa" | "Sterlitamak"))
            if region contains "Bashkortostan" =>
          true
        case _ => false
      )

object IpTrust:

  case class IpData(proxy: IsProxy, location: Location, isTor: Boolean)
