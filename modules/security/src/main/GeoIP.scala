package lila.security

import com.github.blemale.scaffeine.LoadingCache
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.CityResponse
import play.api.ConfigLoader

import scala.util.Try

import lila.common.autoconfig.*
import lila.core.net.IpAddress
import lila.core.security.IsProxy

final class GeoIP(config: GeoIP.Config)(using Executor):

  private val reader: Option[DatabaseReader] =
    try config.file.nonEmpty.option(DatabaseReader.Builder(java.io.File(config.file)).build)
    catch
      case e: Exception =>
        logger.error("MaxMindIpGeo couldn't load", e)
        none

  private val cache: LoadingCache[IpAddress, Option[Location]] =
    lila.memo.CacheApi.scaffeineNoScheduler
      .expireAfterAccess(config.cacheTtl)
      .build(compute)

  private def compute(ip: IpAddress): Option[Location] = for
    r    <- reader
    inet <- ip.inet
    res  <- Try(r.city(inet)).toOption
  yield Location(res)

  def apply(ip: IpAddress): Option[Location] = cache.get(ip)

  def orUnknown(ip: IpAddress): Location = apply(ip) | Location.unknown

  def isSuspicious(ip: IpAddress): Boolean = apply(ip).exists(Location.isSuspicious)

object GeoIP:
  case class Config(
      file: String,
      @ConfigName("cache_ttl") cacheTtl: FiniteDuration
  )
  given ConfigLoader[Config] = AutoConfig.loader

case class Location(
    country: String,
    countryCode: Option[String],
    region: Option[String],
    city: Option[String]
):

  lazy val id = scalalib.StringOps.slug:
    List(shortCountry.some, region, city).flatten.mkString("")

  def shortCountry: String = ~country.split(',').headOption

  override def toString = List(shortCountry.some, region, city).flatten.mkString(" > ")

object Location:

  val unknown = Location("Solar System", none, none, none)

  def apply(res: CityResponse): Location =
    Location(
      Option(res.getCountry).flatMap(c => Option(c.getName)) | unknown.country,
      Option(res.getCountry).flatMap(c => Option(c.getIsoCode)),
      Option(res.getMostSpecificSubdivision).flatMap(s => Option(s.getName())),
      Option(res.getCity).flatMap(c => Option(c.getName))
    )

  def isSuspicious(loc: Location) =
    loc == unknown ||
      loc.region.has("Kirov Oblast") ||
      (loc.region.has("Samsun") && loc.city.has("Samsun"))

  case class WithProxy(location: Location, proxy: IsProxy)
