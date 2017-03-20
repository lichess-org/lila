package lila.common

import scala.concurrent.duration._

case class ApiVersion(value: Int) extends AnyVal with IntValue {
  def v1 = value == 1
  def v2 = value == 2
}

case class AssetVersion(value: Int) extends AnyVal with IntValue

case class MaxPerPage(value: Int) extends AnyVal with IntValue

case class IpAddress(value: String) extends AnyVal with StringValue

object IpAddress {
  // http://stackoverflow.com/questions/106179/regular-expression-to-match-hostname-or-ip-address
  private val ipv4Regex = """^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$""".r
  // ipv6 address in standard form (no compression, no leading zeros)
  private val ipv6Regex = """^((0|[1-9a-f][0-9a-f]{0,3}):){7}(0|[1-9a-f][0-9a-f]{0,3})""".r

  def isv4(a: IpAddress) = ipv4Regex matches a.value
  def isv6(a: IpAddress) = ipv6Regex matches a.value
}

// maximum value = Int.MaxValue / 100 / 60 / 60 / 24 = 248 days
case class Centis(cs: Int) extends AnyVal {

  def toDuration = FiniteDuration(cs * 10, MILLISECONDS)

  def +(other: Centis) = Centis(cs + other.cs)
  def -(other: Centis) = Centis(cs - other.cs)
  def *(scalar: Int) = Centis(scalar * cs)
  def unary_- = Centis(-cs)
}

object Centis {

  def apply(centis: Long): Centis = Centis {
    if (centis > Int.MaxValue) {
      lila.log("common").error(s"Truncating Centis! $centis")
      Int.MaxValue
    }
    else centis.toInt
  }

  def apply(d: FiniteDuration): Centis = Centis {
    if (d.unit eq MILLISECONDS) d.length / 10
    else d.toMillis / 10
  }
}
