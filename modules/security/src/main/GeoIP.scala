package lila.security

import com.snowplowanalytics.maxmind.geoip.{ IpGeo, IpLocation }

import lila.memo.AsyncCache
import scala.concurrent.Future

private[security] final class GeoIP(file: java.io.File, cacheSize: Int) {

  private val ipgeo = new IpGeo(dbFile = file, memCache = false, lruCache = 0)

  private val cache = AsyncCache(
    f = (ip: String) => Future { ipgeo getLocation ip },
    maxCapacity = cacheSize)

  def apply(ip: String): Future[Location] =
    cache(ip) map (_.fold(Location.unknown)(Location.apply))
}

case class Location(
    country: String,
    region: Option[String],
    city: Option[String]) {

  def comparable = (country, ~region, ~city)

  override def toString = List(city, region, country.some).flatten mkString ", "
}

object Location {

  val unknown = Location("Solar System", none, none)

  def apply(ipLoc: IpLocation): Location =
    Location(ipLoc.countryName, ipLoc.region, ipLoc.city)
}
