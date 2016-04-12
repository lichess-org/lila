package lila.security

import com.sanoma.cda.geoip.{ MaxMindIpGeo, IpLocation }

import scala.concurrent.duration._

final class GeoIP(file: String, cacheTtl: Duration) {

  private val geoIp = MaxMindIpGeo(file, 0)
  private val cache = lila.memo.Builder.cache(cacheTtl, compute)

  private def compute(ip: String): Option[Location] =
    geoIp getLocation ip map Location.apply

  def apply(ip: String): Option[Location] = cache get ip

  def orUnknown(ip: String): Location = apply(ip) | Location.unknown
}

case class Location(
    country: String,
    region: Option[String],
    city: Option[String]) {

  def comparable = (country, ~region, ~city)

  def shortCountry: String = ~country.split(',').headOption

  override def toString = List(shortCountry.some, region, city).flatten mkString " > "
}

object Location {

  val unknown = Location("Solar System", none, none)

  val tor = Location("Tor exit node", none, none)

  def apply(ipLoc: IpLocation): Location =
    Location(ipLoc.countryName | unknown.country, ipLoc.region, ipLoc.city)
}
