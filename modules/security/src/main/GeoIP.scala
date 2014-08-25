package lila.security

import com.sanoma.cda.geoip.{ MaxMindIpGeo, IpLocation }

final class GeoIP(file: String, cacheSize: Int) {

  private val geoIp = MaxMindIpGeo(file, cacheSize)

  def apply(ip: String): Option[Location] = geoIp getLocation ip map Location.apply
}

case class Location(
    country: String,
    region: Option[String],
    city: Option[String]) {

  def comparable = (country, ~region, ~city)

  def shortCountry: String = ~country.split(',').headOption

  override def toString = List(shortCountry.some, region, city).flatten mkString " / "
}

object Location {

  val unknown = Location("Solar System", none, none)

  def apply(ipLoc: IpLocation): Location =
    Location(ipLoc.countryName | unknown.country, ipLoc.region, ipLoc.city)
}
