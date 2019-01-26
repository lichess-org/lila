package lila.common

case class ApiVersion(value: Int) extends AnyVal with IntValue with Ordered[ApiVersion] {
  def v1 = value == 1
  def v2 = value == 2
  def v3 = value == 3
  def v4 = value == 4
  def compare(other: ApiVersion) = value compare other.value
  def gt(other: Int) = value > other
  def gte(other: Int) = value >= other
}

case class AssetVersion(value: String) extends AnyVal with StringValue

object AssetVersion {
  var current = random
  def change = { current = random }
  private def random = AssetVersion(ornicar.scalalib.Random secureString 6)
}

case class IsMobile(value: Boolean) extends AnyVal with BooleanValue

case class MaxPerPage(value: Int) extends AnyVal with IntValue

case class MaxPerSecond(value: Int) extends AnyVal with IntValue

case class IpAddress(value: String) extends AnyVal with StringValue

object IpAddress {
  // http://stackoverflow.com/questions/106179/regular-expression-to-match-hostname-or-ip-address
  private val ipv4Regex = """^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$""".r
  // ipv6 address in standard form (no compression, no leading zeros)
  private val ipv6Regex = """^((0|[1-9a-f][0-9a-f]{0,3}+):){7}(0|[1-9a-f][0-9a-f]{0,3})""".r

  def isv4(a: IpAddress) = ipv4Regex matches a.value
  def isv6(a: IpAddress) = ipv6Regex matches a.value

  def from(str: String): Option[IpAddress] = {
    ipv4Regex.matches(str) || ipv6Regex.matches(str)
  } option IpAddress(str)
}

case class EmailAddress(value: String) extends AnyVal with StringValue {
  def conceal = value split '@' match {
    case Array(user, domain) => s"${user take 3}*****@${domain}"
    case _ => value
  }
  def domain: Option[Domain] = value split '@' match {
    case Array(_, domain) => Domain(domain).some
    case _ => none
  }
}

object EmailAddress {

  private val regex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]++@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  def from(str: String): Option[EmailAddress] =
    regex.find(str) option EmailAddress(str)

  private val hotmailRegex = """@(live|hotmail|outlook)\.""".r
}

case class Domain(value: String) extends AnyVal with StringValue

case class Strings(value: List[String]) extends AnyVal
