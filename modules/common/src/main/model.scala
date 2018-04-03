package lila.common

case class ApiVersion(value: Int) extends AnyVal with IntValue {
  def v1 = value == 1
  def v2 = value == 2
  def v3 = value == 3
}

case class AssetVersion(value: Int) extends AnyVal with IntValue

case class MaxPerPage(value: Int) extends AnyVal with IntValue

case class MaxPerSecond(value: Int) extends AnyVal with IntValue

case class IpAddress(value: String) extends AnyVal with StringValue

object IpAddress {
  // http://stackoverflow.com/questions/106179/regular-expression-to-match-hostname-or-ip-address
  private val ipv4Regex = """^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$""".r
  // ipv6 address in standard form (no compression, no leading zeros)
  private val ipv6Regex = """^((0|[1-9a-f][0-9a-f]{0,3}):){7}(0|[1-9a-f][0-9a-f]{0,3})""".r

  def isv4(a: IpAddress) = ipv4Regex matches a.value
  def isv6(a: IpAddress) = ipv6Regex matches a.value

  def from(str: String): Option[IpAddress] = {
    ipv4Regex.matches(str) || ipv6Regex.matches(str)
  } option IpAddress(str)
}

case class EmailAddress(value: String) extends AnyVal with StringValue

object EmailAddress {

  private val regex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  def from(str: String): Option[EmailAddress] =
    regex.matches(str) option EmailAddress(str)
}
