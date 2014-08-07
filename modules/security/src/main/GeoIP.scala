package lila.security

import com.sanoma.cda.geoip.{ MaxMindIpGeo, IpLocation }

import lila.memo.AsyncCache
import scala.concurrent.Future

private[security] final class GeoIP(file: String, cacheSize: Int) {

  private val geoIp = MaxMindIpGeo(file, 0)

  private val cache = AsyncCache(
    f = (ip: String) => Future { geoIp getLocation ip },
    maxCapacity = cacheSize)

  def apply(ip: String): Future[Location] =
    cache(ip) map (_.fold(Location.unknown)(Location.apply))
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
