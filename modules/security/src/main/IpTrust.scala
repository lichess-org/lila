package lila.security

import lila.common.IpAddress

final class IpTrust(proxyApi: Ip2Proxy, geoApi: GeoIP, torApi: Tor, firewallApi: Firewall) {

  def isSuspicious(ip: IpAddress, reason: Ip2Proxy.Reason): Fu[Boolean] =
    if (Ip2Proxy isBlacklisted ip) fuTrue
    else if (firewallApi blocksIp ip) fuTrue
    else if (torApi isExitNode ip) fuTrue
    else {
      val location = geoApi orUnknown ip
      if (location == Location.unknown || location == Location.tor) fuTrue
      else if (isUndetectedProxy(location)) fuTrue
      else proxyApi(ip, reason).dmap(_.isProxy)
    }

  def isSuspicious(ipData: UserSpy.IPData, reason: Ip2Proxy.Reason): Fu[Boolean] =
    isSuspicious(ipData.ip.value, reason)

  /* lichess blacklist of proxies that ip2proxy doesn't know about */
  private def isUndetectedProxy(location: Location): Boolean =
    location.shortCountry == "Iran" ||
      location.shortCountry == "United Arab Emirates" ||
      location == Location("Poland", "Subcarpathian Voivodeship".some, "Stalowa Wola".some) ||
      location == Location("Poland", "Lesser Poland Voivodeship".some, "Krakow".some) ||
      location == Location("Russia", "Bashkortostan".some, "Ufa".some)
}
