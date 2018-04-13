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
      else if (location.shortCountry == "Iran") fuTrue // some undetected proxies
      else if (location.shortCountry == "United Arab Emirates") fuTrue // some undetected proxies
      else intelApi(ip).map { 75 < _ }
    }

  def isSuspicious(ipData: UserSpy.IPData): Fu[Boolean] = isSuspicious(ipData.ip.value)
}
