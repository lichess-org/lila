package lila.worldMap

import com.sanoma.cda.geoip.IpLocation

case class Location(
  country: String,
  lat: Double,
  lon: Double)

object Location {

  def apply(ipLoc: IpLocation): Option[Location] = for {
    country <- ipLoc.countryName
    point <- ipLoc.geoPoint
  } yield Location(country, point.latitude, point.longitude)
}
