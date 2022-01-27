package lila.security

import com.github.blemale.scaffeine.LoadingCache
import com.sanoma.cda.geoip.{ IpLocation, MaxMindIpGeo }
import io.methvin.play.autoconfig._
import scala.concurrent.duration._

import lila.common.IpAddress

final class GeoIP(config: GeoIP.Config) {

  private lazy val geoIp: Option[MaxMindIpGeo] =
    config.file.nonEmpty ?? {
      try {
        val m = MaxMindIpGeo(config.file, 0)
        logger.info("MaxMindIpGeo is enabled")
        m.some
      } catch {
        case e: java.io.FileNotFoundException =>
          logger.info(s"MaxMindIpGeo is disabled: $e")
          none
      }
    }

  private val cache: LoadingCache[IpAddress, Option[Location]] =
    lila.memo.CacheApi.scaffeineNoScheduler
      .expireAfterAccess(config.cacheTtl)
      .build(compute)

  private def compute(ip: IpAddress): Option[Location] =
    geoIp.flatMap(_ getLocation ip.value) map Location.apply

  def apply(ip: IpAddress): Option[Location] = cache get ip

  def orUnknown(ip: IpAddress): Location = apply(ip) | Location.unknown
}

object GeoIP {
  case class Config(
      file: String,
      @ConfigName("cache_ttl") cacheTtl: FiniteDuration
  )
  implicit val configLoader = AutoConfig.loader[Config]
}

case class Location(
    country: String,
    countryCode: Option[String],
    region: Option[String],
    city: Option[String]
) {

  def shortCountry: String = ~country.split(',').headOption

  override def toString = List(shortCountry.some, region, city).flatten mkString " > "
}

object Location {

  val unknown = Location("Solar System", none, none, none)

  val tor = Location("Tor exit node", none, none, none)

  def apply(ipLoc: IpLocation): Location =
    Location(ipLoc.countryName | unknown.country, ipLoc.countryCode, ipLoc.region, ipLoc.city)
}
