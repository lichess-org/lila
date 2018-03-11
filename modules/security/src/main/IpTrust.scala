package lila.security

import lila.common.IpAddress

case class IpTrust(intelApi: IpIntel, geoApi: GeoIP, torApi: Tor, firewallApi: Firewall) {

  def isSuspicious(ip: IpAddress): Fu[Boolean] =
    if (IpIntel isBlacklisted ip) yep
    else if (firewallApi blocksIp ip) yep
    else if (torApi isExitNode ip) yep
    else {
      val location = geoApi orUnknown ip
      if (location == Location.unknown || location == Location.tor) yep
      else if (location.shortCountry == "Iran") yep // some undetected proxies
      else if (location.shortCountry == "United Arab Emirates") yep // some undetected proxies
      else intelApi(ip).map { 75 < _ }
    }

  def isSuspicious(ipData: UserSpy.IPData): Fu[Boolean] = isSuspicious(ipData.ip)

  private val yep = fuccess(true)
}
