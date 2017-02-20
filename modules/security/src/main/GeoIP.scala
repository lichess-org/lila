package lila.security

import com.github.blemale.scaffeine.{ LoadingCache, Scaffeine }
import com.sanoma.cda.geoip.{ MaxMindIpGeo, IpLocation }
import scala.concurrent.duration._

import lila.common.IpAddress

final class GeoIP(file: String, cacheTtl: FiniteDuration) {

  private val geoIp = MaxMindIpGeo(file, 0)

  private val cache: LoadingCache[IpAddress, Option[Location]] = Scaffeine()
    .expireAfterAccess(cacheTtl)
    .build(compute)

  private def compute(ip: IpAddress): Option[Location] =
    geoIp getLocation ip.value map Location.apply

  def apply(ip: IpAddress): Option[Location] = cache get ip

  def orUnknown(ip: IpAddress): Location = apply(ip) | Location.unknown
}

case class Location(
    country: String,
    region: Option[String],
    city: Option[String]
) {

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
