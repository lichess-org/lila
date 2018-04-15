package lila.security

import lila.common.IpAddress

final class IpTrust(intelApi: IpIntel, geoApi: GeoIP, torApi: Tor, firewallApi: Firewall) {

  def isSuspicious(ip: IpAddress): Fu[Boolean] =
    if (IpIntel isBlacklisted ip) fuTrue
    else if (firewallApi blocksIp ip) fuTrue
    else if (torApi isExitNode ip) fuTrue
    else {
      val location = geoApi orUnknown ip
      if (location == Location.unknown || location == Location.tor) fuTrue
      else if (isUndetectedProxy(location)) fuTrue
      else intelApi(ip).map { 75 < _ }
    }

  def isSuspicious(ipData: UserSpy.IPData): Fu[Boolean] = isSuspicious(ipData.ip.value)

  /* lichess blacklist of proxies that ipintel doesn't know about */
  private def isUndetectedProxy(location: Location): Boolean =
    location.shortCountry == "Iran" ||
      location.shortCountry == "United Arab Emirates" ||
      location == Location("Poland", "Subcarpathian Voivodeship".some, "Stalowa Wola".some) ||
      location == Location("Poland", "Lesser Poland Voivodeship".some, "Krakow".some)
}
