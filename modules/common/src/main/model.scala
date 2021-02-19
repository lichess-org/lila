package lila.common

import scala.concurrent.duration._

case class ApiVersion(value: Int) extends AnyVal with IntValue with Ordered[ApiVersion] {
  def compare(other: ApiVersion) = Integer.compare(value, other.value)
  def gt(other: Int)             = value > other
  def gte(other: Int)            = value >= other
  def puzzleV2                   = value >= 6
}

case class AssetVersion(value: String) extends AnyVal with StringValue

object AssetVersion {
  var current = random
  def change() = { current = random }
  private def random = AssetVersion(ornicar.scalalib.Random secureString 6)
}

case class IsMobile(value: Boolean) extends AnyVal with BooleanValue

case class IpAddress(value: String) extends AnyVal with StringValue {
  def blockable =
    value != "127.0.0.1" && value != "0:0:0:0:0:0:0:1" && // loopback
      value != "0.0.0.0" && value != "0:0:0:0:0:0:0:0"    // unspecified
}

object IpAddress {
  // http://stackoverflow.com/questions/106179/regular-expression-to-match-hostname-or-ip-address
  private val ipv4Regex =
    """^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])$""".r
  // ipv6 address in standard form (no compression, no leading zeros!)
  private val ipv6Regex = """^((0|[1-9a-f][0-9a-f]{0,3}+):){7}(0|[1-9a-f][0-9a-f]{0,3})""".r

  def from(str: String): Option[IpAddress] = {
    ipv4Regex.matches(str) || ipv6Regex.matches(str)
  } option IpAddress(str)
}

case class NormalizedEmailAddress(value: String) extends AnyVal with StringValue

case class EmailAddress(value: String) extends AnyVal with StringValue {
  def conceal =
    value split '@' match {
      case Array(user, domain) => s"${user take 3}*****@$domain"
      case _                   => value
    }

  def normalize =
    NormalizedEmailAddress {
      // changing normalization requires database migration!
      val lower = value.toLowerCase
      lower.split('@') match {
        case Array(name, domain) if EmailAddress.gmailLikeNormalizedDomains(domain) =>
          val normalizedName = name
            .replace(".", "")  // remove all dots
            .takeWhile('+' !=) // skip everything after the first '+'
          if (normalizedName.isEmpty) lower else s"$normalizedName@$domain"
        case _ => lower
      }
    }

  def domain: Option[Domain] =
    value split '@' match {
      case Array(_, domain) => Domain from domain.toLowerCase
      case _                => none
    }

  def similarTo(other: EmailAddress) = normalize == other.normalize

  def isNoReply  = EmailAddress isNoReply value
  def isSendable = !isNoReply

  // safer logs
  override def toString = "EmailAddress(****)"
}

object EmailAddress {

  private val regex =
    """^[a-zA-Z0-9\.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$""".r

  // adding normalized domains requires database migration!
  private val gmailLikeNormalizedDomains =
    Set("gmail.com", "googlemail.com", "protonmail.com", "protonmail.ch", "pm.me")

  def isValid(str: String) =
    regex.matches(str) && !str.contains("..") && !str.contains(".@") && !str.startsWith(".")

  def from(str: String): Option[EmailAddress] =
    isValid(str) option EmailAddress(str)

  private def isNoReply(str: String) = str.startsWith("noreply.") && str.endsWith("@lichess.org")
}

case class Domain private (value: String) extends AnyVal with StringValue {
  // heuristic to remove user controlled subdomain tails:
  // tail.domain.com, tail.domain.co.uk, tail.domain.edu.au, etc.
  def withoutSubdomain: Option[Domain] =
    value.split('.').toList.reverse match {
      case tld :: sld :: tail :: _ if sld.lengthIs <= 3 => Domain from s"$tail.$sld.$tld"
      case tld :: sld :: _                              => Domain from s"$sld.$tld"
      case _                                            => none
    }
  def lower = Domain.Lower(value.toLowerCase)
}

object Domain {
  // https://stackoverflow.com/a/26987741/1744715
  private val regex =
    """^(((?!-))(xn--|_{1,1})?[a-z0-9-]{0,61}[a-z0-9]{1,1}\.)*(xn--)?([a-z0-9][a-z0-9\-]{0,60}|[a-z0-9-]{1,30}\.[a-z]{2,})$""".r
  def isValid(str: String)              = regex.matches(str)
  def from(str: String): Option[Domain] = isValid(str) option Domain(str)
  def unsafe(str: String): Domain       = Domain(str)

  case class Lower(value: String) extends AnyVal with StringValue {
    def domain = Domain(value)
  }
}

case class Strings(value: List[String]) extends AnyVal
case class UserIds(value: List[String]) extends AnyVal
case class Ints(value: List[Int])       extends AnyVal

case class Every(value: FiniteDuration)  extends AnyVal
case class AtMost(value: FiniteDuration) extends AnyVal

case class Template(value: String) extends AnyVal
